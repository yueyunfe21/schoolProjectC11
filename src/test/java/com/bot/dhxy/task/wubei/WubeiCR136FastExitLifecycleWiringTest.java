package com.bot.dhxy.task.wubei;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR136 WUBEI expected-combat fast-exit lifecycle.
 */
public final class WubeiCR136FastExitLifecycleWiringTest {

    private WubeiCR136FastExitLifecycleWiringTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String battleRadar = read(root, "src/main/java/com/bot/dhxy/service/BattleRadarService.java");
        String autoCombat = read(root, "src/main/java/com/bot/dhxy/service/AutoCombatService.java");
        String wubei = read(root, "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java");

        battleRadarGatesExpectedExitByArmedWait(battleRadar);
        autoCombatUsesExpectedExitGate(autoCombat);
        wubeiStopsReturnRetriesAfterTrustedInCombat(wubei);
        wubeiRefreshesBaselineAfterTrustedInCombat(wubei, autoCombat, battleRadar);

        System.out.println("WubeiCR136FastExitLifecycleWiringTest passed");
    }

    private static void battleRadarGatesExpectedExitByArmedWait(String battleRadar) {
        require(battleRadar.contains("public void armExpectedCombatExitWait(String source)"),
                "BattleRadarService must expose an expected-combat wait arm point");
        require(battleRadar.contains("expectedCombatExitWaitArmedAtMs"),
                "BattleRadarService must remember when the current expected wait was armed");
        require(battleRadar.contains("combatExitPendingAtMs"),
                "BattleRadarService must timestamp pending combat exits");
        String consume = methodBody(battleRadar, "public boolean consumeCombatExitSignalForExpectedWait(");
        require(consume.contains("combatExitPendingAtMs < state.expectedCombatExitWaitArmedAtMs"),
                "Expected waits must reject exit signals older than the current arm point");
        require(consume.contains("state.combatExitPending = false"),
                "Rejected stale expected exits must be cleared instead of left for the next tick");
        require(consume.contains("return true"),
                "Fresh expected exits must still be consumable");
    }

    private static void autoCombatUsesExpectedExitGate(String autoCombat) {
        require(autoCombat.contains("expectedCombatExitWaitArmed"),
                "AutoCombatService must track whether the current expected wait has been armed");
        String tick = methodBody(autoCombat,
                "public TickResult handleCombatTick(TaskExecutionContext context,\n"
                        + "                                       String source,\n"
                        + "                                       PostCombatRecoveryPolicy recoveryPolicy)");
        require(tick.contains("safePolicy == PostCombatRecoveryPolicy.FAST_EXPECTED_EXIT")
                        && tick.contains("battleRadarService.armExpectedCombatExitWait(source)"),
                "FAST_EXPECTED_EXIT ticks must arm the current expected wait before consuming exits");
        String consume = methodBody(autoCombat, "private boolean consumeExitAndRecover(");
        require(consume.contains("battleRadarService.consumeCombatExitSignalForExpectedWait(source)"),
                "FAST_EXPECTED_EXIT must consume through the expected-wait stale-exit gate");
        require(consume.contains("state.expectedCombatExitWaitArmed = false"),
                "Consumed exits must clear the expected-wait arm for the next combat");
    }

    private static void wubeiStopsReturnRetriesAfterTrustedInCombat(String wubei) {
        require(wubei.contains("enum ReturnHomeResult"),
                "Wubei return-home helper must distinguish verified, failed, and trusted-IN_COMBAT correction");
        String useOne = methodBody(wubei,
                "private ReturnItemUseResult useReturnItem(");
        int cachedUnverified = indexOf(useOne, "cached return item used but start map not verified");
        int fullScan = indexOf(useOne, "bagService.findAndUseMainBagTaskPageItem");
        int cachedReturn = indexOf(useOne, "ReturnItemUseResult.usedStartMapUnverified");
        require(cachedReturn > cachedUnverified && cachedReturn < fullScan,
                "A cached return click that fails start-map verification must not fall through to full bag scan first");

        String useAll = methodBody(wubei,
                "private ReturnHomeResult useReturnItemAndVerifyStartMap(");
        require(useAll.contains("ReturnItemUseResult.Status.USED_START_MAP_UNVERIFIED"),
                "Return-home loop must notice that a return item was actually used but unverified");
        require(useAll.contains("probeTrustedCombatStateAfterReturnVerificationFailure("),
                "Return-home loop must run the trusted read-only combat probe immediately after an unverified use");
        int unverified = indexOf(useAll, "ReturnItemUseResult.Status.USED_START_MAP_UNVERIFIED");
        int probe = indexOf(useAll, "probeTrustedCombatStateAfterReturnVerificationFailure(");
        int stillCombat = indexOf(useAll, "ReturnHomeResult.STILL_IN_COMBAT", probe);
        int failedAfterProbe = indexOf(useAll, "return ReturnHomeResult.FAILED_AFTER_TRUSTED_NOT_IN_COMBAT;", stillCombat);
        int nextAttempt = useAll.indexOf("for (int attempt", unverified + 1);
        require(probe > unverified && stillCombat > probe && (nextAttempt < 0 || stillCombat < nextAttempt),
                "Trusted IN_COMBAT must stop remaining return attempts before the helper loops again");
        require(failedAfterProbe > stillCombat,
                "Trusted non-combat after an actual unverified return-item use must end the helper as FAILED");
        int loopEnd = useAll.lastIndexOf("pendingTeamReturnPrecheck = null;");
        require(failedAfterProbe < loopEnd,
                "Actual unverified return-item use must not fall through to the loop-level failure after another attempt");
        require(wubei.contains("FAILED_AFTER_TRUSTED_NOT_IN_COMBAT"),
                "Wubei must distinguish an episode that already used one return item from old no-use failures");
    }

    private static void wubeiRefreshesBaselineAfterTrustedInCombat(String wubei,
                                                                   String autoCombat,
                                                                   String battleRadar) {
        require(autoCombat.contains("refreshFastExpectedExitBaselineAfterTrustedInCombat"),
                "AutoCombatService must expose a trusted-IN_COMBAT baseline refresh");
        require(battleRadar.contains("refreshFastExpectedCombatExitAvatarBaseline"),
                "BattleRadarService must be able to replace the avatar baseline with the current in-combat frame");
        String probe = methodBody(wubei,
                "private AutoCombatService.TickResult probeTrustedCombatStateAfterReturnVerificationFailure(");
        require(probe.contains("probeWindowCombatStateReadOnly("),
                "Wubei trusted correction must use the read-only combat probe");
        require(probe.contains("AutoCombatService.TickResult.IN_COMBAT"),
                "Wubei correction must branch on trusted IN_COMBAT");
        require(probe.contains("refreshFastExpectedExitBaselineAfterTrustedInCombat"),
                "Trusted IN_COMBAT correction must refresh the current combat avatar baseline");
        require(!probe.toLowerCase().contains("disable")
                        && !probe.toLowerCase().contains("degrade")
                        && !probe.toLowerCase().contains("invalid"),
                "CR136 must not disable, degrade, or invalidate same-combat avatar fast-exit");
    }

    private static String read(Path root, String relativePath) throws Exception {
        return Files.readString(root.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static String methodBody(String source, String signature) {
        int signatureIndex = source.indexOf(signature);
        if (signatureIndex < 0) {
            throw new AssertionError("Missing source marker: " + signature);
        }
        int bodyStart = source.indexOf('{', signatureIndex);
        if (bodyStart < 0) {
            throw new AssertionError("Missing method body for: " + signature);
        }
        int depth = 0;
        for (int i = bodyStart; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(bodyStart, i + 1);
                }
            }
        }
        throw new AssertionError("Unclosed method body for: " + signature);
    }

    private static int indexOf(String source, String needle) {
        return indexOf(source, needle, 0);
    }

    private static int indexOf(String source, String needle, int fromIndex) {
        int index = source.indexOf(needle);
        if (fromIndex > 0) {
            index = source.indexOf(needle, fromIndex);
        }
        if (index < 0) {
            throw new AssertionError("Missing source marker: " + needle);
        }
        return index;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
