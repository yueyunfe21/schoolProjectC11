package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR71.
 *
 * <p>Unknown-combat recovery sends heavy foreground input such as task-panel capture and return
 * item use. It must never run while the current bound window still has active combat evidence.</p>
 */
public class XiuluoActiveCombatUnknownExitGuardWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String xiuluo = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8);
        String autoCombat = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/service/AutoCombatService.java"), StandardCharsets.UTF_8);
        String battleRadar = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/service/BattleRadarService.java"), StandardCharsets.UTF_8);

        String waitCombat = between(xiuluo,
                "private XiuluoStepOutcome waitCombat(",
                "private XiuluoStepOutcome waitForCombatStateWake(");
        require(waitCombat.contains("suppressUnknownCombatExitIfActiveCombat(")
                        && waitCombat.contains("\"wait-combat-exit-recovered\", state"),
                "WAIT_COMBAT must suppress stale EXIT_RECOVERED before unknown-combat recovery");
        require(waitCombat.indexOf("suppressUnknownCombatExitIfActiveCombat(")
                        < waitCombat.indexOf("resolveUnknownCombatExit(context, state)"),
                "active-combat guard must run before resolveUnknownCombatExit");

        String unknownExit = between(xiuluo,
                "private XiuluoStepOutcome resolveUnknownCombatExit(",
                "private XiuluoStepOutcome attemptVerifiedReturnAfterUnknownCombat(");
        require(unknownExit.contains("suppressUnknownCombatExitIfActiveCombat(")
                        && unknownExit.contains("\"resolve-unknown-combat-entry\", state"),
                "resolveUnknownCombatExit must guard before location/task-panel/return fallback");
        require(unknownExit.indexOf("\"resolve-unknown-combat-entry\", state")
                        < unknownExit.indexOf("playerStateService.syncMyPosition()"),
                "resolve guard must run before location scan");
        require(unknownExit.indexOf("\"resolve-unknown-combat-before-task-panel\", state")
                        < unknownExit.indexOf("tryReadObjectiveFromTaskPanel("),
                "resolve guard must run before task-panel capture");
        require(unknownExit.indexOf("\"resolve-unknown-combat-before-return\", state")
                        < unknownExit.indexOf("attemptVerifiedReturnAfterUnknownCombat("),
                "resolve guard must run before return-item fallback");

        String returnFallback = between(xiuluo,
                "private XiuluoStepOutcome attemptVerifiedReturnAfterUnknownCombat(",
                "private XiuluoStepOutcome returnHome(");
        require(returnFallback.indexOf("\"unknown-combat-return-attempt\" + attempt, state")
                        < returnFallback.indexOf("useReturnItemAndVerifyStartMap("),
                "return fallback must guard before using the return item");

        require(xiuluo.contains("stale/contradictory unknown-combat exit suppressed"),
                "suppression log must be visible for future runtime audits");
        require(xiuluo.contains("state.withXiuluoBattleStarted(XiuluoPhase.WAIT_COMBAT, \"stale-unknown-combat-exit-suppressed\")"),
                "suppressed stale exit should mark current combat as started before returning to WAIT_COMBAT");

        String handleCombatTick = between(autoCombat,
                "public TickResult handleCombatTick(",
                "public TickResult handleWindowCombatGuardTick(");
        require(handleCombatTick.indexOf("battleRadarService.discardStaleCombatExitSignalIfInCombat(source)")
                        < handleCombatTick.indexOf("consumeExitAndRecover("),
                "AutoCombatService must discard stale exit signals before consuming exit recovery");

        String onEnterCombat = between(battleRadar,
                "private void onEnterCombat()",
                "private void onExitCombat()");
        require(onEnterCombat.contains("state.combatExitPending = false"),
                "combat entry must clear any stale combat-exit signal");
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
