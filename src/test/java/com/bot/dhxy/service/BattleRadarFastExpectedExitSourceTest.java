package com.bot.dhxy.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BattleRadarFastExpectedExitSourceTest {

    public static void main(String[] args) throws Exception {
        String battleRadar = read("src/main/java/com/bot/dhxy/service/BattleRadarService.java");
        String autoCombat = read("src/main/java/com/bot/dhxy/service/AutoCombatService.java");
        String xiuluoTask = read("src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java");

        assertContains(battleRadar, "FAST_EXPECTED_EXIT_PROBE_DELAY_MS = 15_000L",
                "fast expected exit probe must keep the approved 15s combat-entry grace");
        assertContains(battleRadar, "FAST_EXPECTED_EXIT_PROBE_INTERVAL_MS = 1_000L",
                "fast expected exit probe must keep the approved 1s cadence");
        assertContains(battleRadar, "FAST_EXPECTED_EXIT_AVATAR_ROI_SIZE = 20",
                "fast expected exit probe must use the approved 20x20 avatar ROI");
        assertContains(battleRadar, "checkFastExpectedCombatExitByAvatarDiff",
                "BattleRadarService must expose the CR113 fast expected-combat probe");
        assertContains(battleRadar, "ImageFinder.isMatch(state.fastExpectedExitBaselineImage, current",
                "fast expected exit probe must reuse ImageFinder pixel-diff matching");
        assertContains(battleRadar, "return updateCombatState(false)",
                "fast expected exit probe must publish through the existing combat-exit state path");
        assertNotContains(battleRadar, "FAST_EXPECTED_EXIT_CONFIRMATION_COUNT",
                "CR113 must not add an unapproved consecutive-confirmation gate");
        assertNotContains(battleRadar, "detectCombatScreenSignalForFastExit",
                "CR113 must not add an unapproved publish-time full-radar/UI recheck");

        assertContains(autoCombat, "checkFastExpectedCombatExitByAvatarDiff",
                "AutoCombatService must call the fast expected exit probe for expected-combat waits");
        assertContains(autoCombat, "shouldRunFullRadarForFastExpectedExitFallback",
                "AutoCombatService must keep the old full radar as a sparse fallback");
        assertContains(autoCombat, "nextFastExpectedCombatExitProbeDelayMs",
                "AutoCombatService must expose fast-probe wake timing to task waits");
        assertContains(xiuluoTask, "nextCombatWakeDelayMs",
                "Xiuluo WAIT_COMBAT must use the combined combat wake delay, not maintenance-only delay");
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private static void assertContains(String source, String needle, String message) {
        if (!source.contains(needle)) {
            throw new AssertionError(message + "; missing: " + needle);
        }
    }

    private static void assertNotContains(String source, String needle, String message) {
        if (source.contains(needle)) {
            throw new AssertionError(message + "; forbidden: " + needle);
        }
    }
}
