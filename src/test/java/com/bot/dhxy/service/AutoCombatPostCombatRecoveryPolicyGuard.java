package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR109.
 *
 * <p>The card is about preserving latency on expected 修罗/五倍 combat exits: those exits must
 * return to the owning task before synchronous HP/MP or 摄妖香 recovery. This guard intentionally
 * checks the wiring boundary instead of launching game-window services.</p>
 */
public final class AutoCombatPostCombatRecoveryPolicyGuard {

    private static final Path AUTO_COMBAT = Path.of(
            "src/main/java/com/bot/dhxy/service/AutoCombatService.java");
    private static final Path XIULUO = Path.of(
            "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java");
    private static final Path WUBEI = Path.of(
            "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java");

    private AutoCombatPostCombatRecoveryPolicyGuard() {
    }

    public static void main(String[] args) throws Exception {
        String autoCombat = read(AUTO_COMBAT);
        String xiuluo = read(XIULUO);
        String wubei = read(WUBEI);

        require(autoCombat.contains("enum PostCombatRecoveryPolicy"),
                "AutoCombatService must expose an explicit post-combat recovery policy enum");
        require(autoCombat.contains("FAST_EXPECTED_EXIT"),
                "AutoCombatService must support FAST_EXPECTED_EXIT");
        require(autoCombat.contains("FULL_RECOVERY"),
                "AutoCombatService must keep FULL_RECOVERY for incidental/unknown combat");
        require(autoCombat.contains("FULL_RECOVERY_WITH_LEADER_INCENSE"),
                "legacy leader-task callers must keep conservative incense recovery when requested");
        require(autoCombat.contains("pendingLeaderPostCombatRecovery"),
                "Fast expected exits must record a pending deferred leader recovery check");
        require(autoCombat.contains("consumePendingLeaderPostCombatRecoveryIfAllowed"),
                "Pending deferred recovery must have an explicit later safe-point consumer");
        require(autoCombat.contains("legacyPostCombatRecoveryPolicy(checkSheYaoXiangForLeaderTask)"),
                "legacy boolean handleCombatTick path must map to explicit conservative policies");

        require(xiuluo.contains("XiuluoCombatSource.TRACKER_CONFIRM")
                        && xiuluo.contains("PostCombatRecoveryPolicy.FAST_EXPECTED_EXIT"),
                "confirmed Xiuluo tracker combat must use FAST_EXPECTED_EXIT");
        require(xiuluo.contains("XiuluoCombatSource.INCIDENTAL")
                        && xiuluo.contains("PostCombatRecoveryPolicy.FULL_RECOVERY"),
                "incidental Xiuluo combat must stay FULL_RECOVERY");
        String xiuluoReturnHome = between(xiuluo,
                "private XiuluoStepOutcome returnHome(",
                "private void consumeDeferredPostCombatRecoveryDuringNextTaskProgress(");
        require(!xiuluoReturnHome.contains("consumePendingLeaderPostCombatRecoveryIfAllowed"),
                "Xiuluo must not consume deferred recovery immediately after verified return-home");
        require(xiuluo.contains("consumeDeferredPostCombatRecoveryDuringNextTaskProgress(context, \"xiuluo-v2:start-exit-prepath\")"),
                "Xiuluo must consume deferred recovery after the next accepted task starts leaving the start map");
        require(xiuluo.contains("consumeDeferredPostCombatRecoveryDuringNextTaskProgress(context, \"xiuluo-v2:tracker-shortcut-green-clicked\")"),
                "Xiuluo must consume deferred recovery after tracker shortcut movement starts");
        require(xiuluo.contains("consumeDeferredPostCombatRecoveryDuringNextTaskProgress(context, \"xiuluo-v2:target-navigation-pathing-started\")"),
                "Xiuluo must consume deferred recovery after target navigation starts");

        require(wubei.contains("PostCombatRecoveryPolicy.FAST_EXPECTED_EXIT"),
                "expected Wubei WAIT_BATTLE_FINISH exits must use FAST_EXPECTED_EXIT");
        require(wubei.contains("PostCombatRecoveryPolicy.FULL_RECOVERY"),
                "Wubei unexpected/enter-battle exit checks must keep FULL_RECOVERY");
        String wubeiReturnHome = between(wubei,
                "private boolean useReturnItemAndVerifyStartMap(",
                "private WubeiStepOutcome returnHomeAfterCombatOrContinueSpecialTarget(");
        require(!wubeiReturnHome.contains("consumePendingLeaderPostCombatRecoveryIfAllowed"),
                "Wubei must not consume deferred recovery immediately after verified return-home");
        require(wubei.contains("consumePendingLeaderPostCombatRecoveryIfAllowed(\n                context, \"wubei:after-task-accepted\")"),
                "Wubei must consume deferred recovery after the next task is accepted");
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
