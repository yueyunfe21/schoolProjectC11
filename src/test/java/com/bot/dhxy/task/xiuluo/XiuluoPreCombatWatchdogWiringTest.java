package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source/state guard for CR80 修罗 pre-combat watchdog.
 *
 * <p>The watchdog is round-local business safety: retry/recovery jumps before true combat must
 * share one deadline, true combat entry must stop the watchdog, and timeout recovery must reuse the
 * existing phase-failure/reaccept path instead of using the return item.</p>
 */
public class XiuluoPreCombatWatchdogWiringTest {

    public static void main(String[] args) throws Exception {
        assertRoundDeadlineIsSharedAcrossPreCombatStateCopies();
        assertTaskLoopUsesFailureReacceptPathWithoutReturnItem();
    }

    private static void assertRoundDeadlineIsSharedAcrossPreCombatStateCopies() {
        XiuluoRoundContext start = XiuluoRoundContext.start(8);
        long deadlineStartedAt = start.preCombatStartedAtMs();
        require(deadlineStartedAt > 0L, "round start must initialize a pre-combat watchdog timestamp");

        require(start.retrySamePhase("retry").preCombatStartedAtMs() == deadlineStartedAt,
                "retrySamePhase must preserve the same pre-combat deadline");
        require(start.recoverTo(XiuluoPhase.NAVIGATE_TO_TARGET, "recover").preCombatStartedAtMs() == deadlineStartedAt,
                "recoverTo must preserve the same pre-combat deadline");
        require(start.recoverToWithObjective(XiuluoPhase.CLICK_TARGET_NPC, null, "recover-objective")
                        .preCombatStartedAtMs() == deadlineStartedAt,
                "recoverToWithObjective must preserve the same pre-combat deadline");
        require(start.waitForPathing("pathing").preCombatStartedAtMs() == deadlineStartedAt,
                "waitForPathing must preserve the same pre-combat deadline");
        require(start.clearPathingWait("clear").preCombatStartedAtMs() == deadlineStartedAt,
                "clearPathingWait must preserve the same pre-combat deadline");

        XiuluoRoundContext combat = start.withXiuluoBattleStarted(XiuluoPhase.WAIT_COMBAT, "combat");
        require(combat.enteredBattleByXiuluo(), "true combat entry must be recorded on the round context");
        require(combat.preCombatStartedAtMs() == deadlineStartedAt,
                "battle-start context should preserve timestamp for diagnostics even though watchdog is stopped");
    }

    private static void assertTaskLoopUsesFailureReacceptPathWithoutReturnItem() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String task = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8);

        require(task.contains("PRE_COMBAT_WATCHDOG_TIMEOUT_MS = 180_000L"),
                "CR80 requires a 180s pre-combat watchdog constant");
        require(task.contains("xiuluo pre-combat watchdog timeout: round={} phase={} elapsedMs={} limitMs=180000 source={}"),
                "CR80 requires the exact watchdog timeout diagnostic log");

        String loopBeforeTransaction = between(task,
                "while (!roundContext.phase().isTerminal()) {",
                "TaskTransactionOutcome transaction;");
        require(loopBeforeTransaction.contains("checkPreCombatWatchdogTimeout(currentContext)"),
                "round loop must check the watchdog before executing another pre-combat phase");
        require(loopBeforeTransaction.contains("restartRoundAfterPhaseFailure("),
                "watchdog timeout must route through the existing phase-failure/reaccept restart path");
        require(loopBeforeTransaction.contains("roundTrace.addPhaseOutcome(currentContext, watchdogOutcome, TaskTransactionResult.FAILED)"),
                "watchdog timeout must be recorded in the same round trace as a failed phase outcome");

        String watchdogMethod = between(task,
                "private XiuluoStepOutcome checkPreCombatWatchdogTimeout(",
                "private boolean shouldApplyPreCombatWatchdog(");
        require(watchdogMethod.contains("XiuluoStepOutcome.failed("),
                "watchdog timeout must produce a normal FAILED phase outcome");
        require(!watchdogMethod.contains("useReturnItemAndVerifyStartMap"),
                "watchdog timeout must not use the 修罗 return item");

        String policyMethod = between(task,
                "private boolean shouldApplyPreCombatWatchdog(",
                "private XiuluoStepOutcome runPhase(");
        require(policyMethod.contains("state.enteredBattleByXiuluo()"),
                "watchdog must stop once true 修罗 battle entry is observed");
        require(policyMethod.contains("WAIT_COMBAT"),
                "WAIT_COMBAT must be explicitly excluded so hot-start/entry detection can observe combat");
        require(policyMethod.contains("RETURN_HOME"),
                "post-combat return phases must be explicitly excluded from the watchdog");
        require(policyMethod.contains("WAIT_TEAM_RETURN"),
                "team-return phases must be explicitly excluded from the watchdog");
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
