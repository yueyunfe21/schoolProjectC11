package com.bot.dhxy.task;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR121.
 *
 * <p>Expected 修罗/五倍 combat exits may use the fast avatar-diff shortcut, but failed return-home
 * verification must re-check trusted combat state before normal return-failure recovery. This
 * guard keeps that correction wired without launching live game-window services.</p>
 */
public final class ExpectedCombatReturnVerificationCorrectionGuard {

    private static final Path XIULUO = Path.of(
            "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java");
    private static final Path WUBEI = Path.of(
            "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java");

    private ExpectedCombatReturnVerificationCorrectionGuard() {
    }

    public static void main(String[] args) throws Exception {
        String xiuluo = read(XIULUO);
        String wubei = read(WUBEI);

        String xiuluoReturnHome = between(xiuluo,
                "private XiuluoStepOutcome returnHome(",
                "private void consumeDeferredPostCombatRecoveryDuringNextTaskProgress(");
        require(xiuluoReturnHome.indexOf("correctKnownCombatReturnFailureIfStillInCombat(")
                        < xiuluoReturnHome.indexOf("recoverReturnHomeFailure(context, state)"),
                "Xiuluo known-combat return failure must correct active combat before normal recovery");

        String xiuluoCorrection = between(xiuluo,
                "private Optional<XiuluoStepOutcome> correctKnownCombatReturnFailureIfStillInCombat(",
                "private void consumeDeferredPostCombatRecoveryDuringNextTaskProgress(");
        require(xiuluoCorrection.contains("probeWindowCombatStateReadOnly("),
                "Xiuluo correction must use trusted read-only combat probe");
        require(xiuluoCorrection.contains("AutoCombatService.TickResult.IN_COMBAT"),
                "Xiuluo correction must branch on trusted IN_COMBAT");
        require(xiuluoCorrection.contains("XiuluoPhase.WAIT_COMBAT"),
                "Xiuluo active-combat correction must return to WAIT_COMBAT");
        require(!xiuluoCorrection.contains("consumePendingLeaderPostCombatRecoveryIfAllowed"),
                "Xiuluo active-combat correction must not consume deferred leader recovery");

        String wubeiCorrection = between(wubei,
                "private WubeiStepOutcome correctExpectedReturnFailureIfStillInCombat(",
                "private boolean continueChainedCombatFromTracker(");
        require(wubeiCorrection.contains("probeWindowCombatStateReadOnly("),
                "Wubei correction must use trusted read-only combat probe");
        require(wubeiCorrection.contains("AutoCombatService.TickResult.IN_COMBAT"),
                "Wubei correction must branch on trusted IN_COMBAT");
        require(wubeiCorrection.contains("WubeiPhase.WAIT_BATTLE_FINISH"),
                "Wubei active-combat correction must return to WAIT_BATTLE_FINISH");
        require(!wubeiCorrection.contains("consumePendingLeaderPostCombatRecoveryIfAllowed"),
                "Wubei active-combat correction must not consume deferred leader recovery");

        require(wubeiReturnFailureChecksCorrection(wubei, "normal-combat"),
                "Normal Wubei return failure must run correction before failing");
        require(wubeiReturnFailureChecksCorrection(wubei, "chained-combat-fast-miss"),
                "Chained fast-miss return failure must run correction before failing");
        require(wubeiReturnFailureChecksCorrection(wubei, "chained-combat-title-gone"),
                "Chained title-gone return failure must run correction before failing");
        require(wubeiReturnFailureChecksCorrection(wubei, "chained-combat-completed"),
                "Chained completed return failure must run correction before failing");
    }

    private static boolean wubeiReturnFailureChecksCorrection(String source, String returnSource) {
        int returnIndex = source.indexOf("useReturnItemAndVerifyStartMap(context, \"" + returnSource + "\")");
        if (returnIndex < 0) {
            return false;
        }
        int failedIndex = source.indexOf("return WubeiStepOutcome.failed(state, \"return home failed\")", returnIndex);
        int correctionIndex = source.indexOf("correctExpectedReturnFailureIfStillInCombat(", returnIndex);
        return failedIndex > returnIndex && correctionIndex > returnIndex && correctionIndex < failedIndex;
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
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
