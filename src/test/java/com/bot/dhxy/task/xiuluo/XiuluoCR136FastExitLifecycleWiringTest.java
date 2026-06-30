package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR136 Xiuluo expected-combat fast-exit correction parity.
 */
public final class XiuluoCR136FastExitLifecycleWiringTest {

    private XiuluoCR136FastExitLifecycleWiringTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String xiuluo = read(root, "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java");
        String autoCombat = read(root, "src/main/java/com/bot/dhxy/service/AutoCombatService.java");

        xiuluoStopsReturnRetriesAfterActualUnverifiedUse(xiuluo);
        xiuluoRefreshesBaselineAfterTrustedInCombat(xiuluo, autoCombat);

        System.out.println("XiuluoCR136FastExitLifecycleWiringTest passed");
    }

    private static void xiuluoStopsReturnRetriesAfterActualUnverifiedUse(String xiuluo) {
        require(xiuluo.contains("enum ReturnHomeResult"),
                "Xiuluo return-home helper must distinguish verified, trusted-IN_COMBAT, and actual-use failures");
        require(xiuluo.contains("record ReturnItemUseResult"),
                "Xiuluo return-item click helper must report whether a return item was actually used");
        require(!xiuluo.contains("ReturnItemUseResult.Status")
                        && !xiuluo.contains("private enum Status"),
                "CR136 return-item result must not depend on an extra nested Status enum class");

        String useOne = methodBody(xiuluo,
                "private ReturnItemUseResult useReturnItem(");
        int cachedUnverified = indexOf(useOne, "cached return item used but start map not verified");
        int cachedReturn = indexOf(useOne, "ReturnItemUseResult.usedStartMapUnverified");
        int fullScan = indexOf(useOne, "bagService.findAndUseMainBagTaskPageItem");
        require(cachedReturn > cachedUnverified && cachedReturn < fullScan,
                "A cached Xiuluo return click that fails start-map verification must not fall through to full scan");

        String useAll = methodBody(xiuluo,
                "private ReturnHomeResult useReturnItemAndVerifyStartMap(");
        require(useAll.contains("result.usedStartMapUnverified()"),
                "Xiuluo return-home loop must notice that a return item was actually used but unverified");
        require(useAll.contains("probeTrustedCombatStateAfterReturnVerificationFailure("),
                "Xiuluo return-home loop must run trusted combat probe immediately after an unverified use");
        int unverified = indexOf(useAll, "result.usedStartMapUnverified()");
        int probe = indexOf(useAll, "probeTrustedCombatStateAfterReturnVerificationFailure(", unverified);
        int stillCombat = indexOf(useAll, "ReturnHomeResult.STILL_IN_COMBAT", probe);
        int failedAfterProbe = indexOf(useAll, "return ReturnHomeResult.FAILED_AFTER_TRUSTED_NOT_IN_COMBAT;", stillCombat);
        int nextAttempt = useAll.indexOf("for (int attempt", unverified + 1);
        require(probe > unverified && stillCombat > probe && (nextAttempt < 0 || stillCombat < nextAttempt),
                "Trusted IN_COMBAT must stop remaining Xiuluo return attempts before the helper loops again");
        require(failedAfterProbe > stillCombat,
                "Trusted non-combat after an actual unverified Xiuluo return-item use must end the helper as FAILED");

        String returnHome = methodBody(xiuluo, "private XiuluoStepOutcome returnHome(");
        int result = indexOf(returnHome, "ReturnHomeResult returnHome = useReturnItemAndVerifyStartMap(");
        int stillInCombatBranch = indexOf(returnHome, "returnHome == ReturnHomeResult.STILL_IN_COMBAT", result);
        int recover = indexOf(returnHome, "recoverReturnHomeFailure(context, state)", result);
        require(stillInCombatBranch > result && stillInCombatBranch < recover,
                "Xiuluo returnHome must resume WAIT_COMBAT before generic return-home retry after trusted IN_COMBAT");
        require(returnHome.contains("returnHome == ReturnHomeResult.FAILED_AFTER_TRUSTED_NOT_IN_COMBAT"),
                "Xiuluo returnHome must not run generic phase retry after one actual unverified return-item use");
    }

    private static void xiuluoRefreshesBaselineAfterTrustedInCombat(String xiuluo, String autoCombat) {
        require(autoCombat.contains("refreshFastExpectedExitBaselineAfterTrustedInCombat"),
                "AutoCombatService must expose a trusted-IN_COMBAT baseline refresh");
        String probe = methodBody(xiuluo,
                "private AutoCombatService.TickResult probeTrustedCombatStateAfterReturnVerificationFailure(");
        require(probe.contains("probeWindowCombatStateReadOnly("),
                "Xiuluo trusted correction must use the read-only combat probe");
        require(probe.contains("AutoCombatService.TickResult.IN_COMBAT"),
                "Xiuluo correction must branch on trusted IN_COMBAT");
        require(probe.contains("refreshFastExpectedExitBaselineAfterTrustedInCombat"),
                "Xiuluo trusted-IN_COMBAT correction must refresh the current combat avatar baseline");
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
                    return source.substring(signatureIndex, i + 1);
                }
            }
        }
        throw new AssertionError("Unclosed method body for: " + signature);
    }

    private static int indexOf(String source, String needle) {
        return indexOf(source, needle, 0);
    }

    private static int indexOf(String source, String needle, int fromIndex) {
        int index = source.indexOf(needle, fromIndex);
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
