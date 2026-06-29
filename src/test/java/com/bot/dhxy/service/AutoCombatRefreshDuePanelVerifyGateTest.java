package com.bot.dhxy.service;

import java.nio.file.Files;
import java.nio.file.Path;

public class AutoCombatRefreshDuePanelVerifyGateTest {

    public static void main(String[] args) throws Exception {
        classifiesCachedRoundStateBeforePanelVerify();
        refreshDuePanelVerifyDefersSameTeamBeforeHeavyScan();
        autoCombatServiceGatesRefreshDueBeforeHeavyPanelVerify();
        entryMaintenanceVerifiesPanelWithoutRoundRefresh();
        entryMaintenanceDoesNotReturnBeforeOptionalRefresh();
        entryMaintenanceForcesActualRoundReadBeforeTrustingHealthyCache();
        entryMaintenanceMergesAllowedRefreshDueVerify();
        verifyAndRefreshReadsVisibleRoundsBeforeTrustingHealthyCache();
    }

    private static void classifiesCachedRoundStateBeforePanelVerify() {
        assertEquals("healthy cached estimate needs no panel verify",
                null,
                AutoCombatPanelService.resolveRoundsRefreshReason(25, 1_000L, 30_000L, 20_000L));
        assertEquals("unknown rounds bypass optional refresh-due gate",
                AutoCombatPanelService.RoundsRefreshReason.UNKNOWN,
                AutoCombatPanelService.resolveRoundsRefreshReason(-1, 1_000L, 30_000L, 20_000L));
        assertEquals("low rounds bypass optional refresh-due gate",
                AutoCombatPanelService.RoundsRefreshReason.LOW_ROUNDS,
                AutoCombatPanelService.resolveRoundsRefreshReason(10, 1_000L, 30_000L, 20_000L));
        assertEquals("refresh due is the only reason that should be team-gated before verify",
                AutoCombatPanelService.RoundsRefreshReason.REFRESH_DUE,
                AutoCombatPanelService.resolveRoundsRefreshReason(25, 1_000L, 30_000L, 31_000L));
    }

    private static void refreshDuePanelVerifyDefersSameTeamBeforeHeavyScan() {
        AutoCombatService.RefreshDuePanelVerifyGate gate = new AutoCombatService.RefreshDuePanelVerifyGate();

        AutoCombatService.RefreshDuePanelVerifyDecision first =
                gate.reserveIfAllowed("xiuluo_v2", "hwnd-A", 1_000L);
        AutoCombatService.RefreshDuePanelVerifyDecision second =
                gate.reserveIfAllowed("xiuluo_v2", "hwnd-B", 20_000L);
        AutoCombatService.RefreshDuePanelVerifyDecision differentTeam =
                gate.reserveIfAllowed("wubei", "hwnd-C", 21_000L);
        AutoCombatService.RefreshDuePanelVerifyDecision afterWindow =
                gate.reserveIfAllowed("xiuluo_v2", "hwnd-D", 31_000L);

        assertFalse("first refresh-due verify is allowed", first.deferred());
        assertTrue("same team refresh-due verify inside 30s is deferred", second.deferred());
        assertEquals("defer retry-after", 11_000L, second.retryAfterMs());
        assertFalse("different team refresh-due verify is independent", differentTeam.deferred());
        assertFalse("same team refresh-due verify after 30s is allowed", afterWindow.deferred());
    }

    private static void autoCombatServiceGatesRefreshDueBeforeHeavyPanelVerify() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/bot/dhxy/service/AutoCombatService.java"));
        int optionalRefreshIndex = source.indexOf("// Auto panel refresh is optional");
        int refreshDueBranchIndex = source.indexOf("refreshReason == AutoCombatPanelService.RoundsRefreshReason.REFRESH_DUE", optionalRefreshIndex);
        int gateIndex = source.indexOf("refreshDuePanelVerifyGate.reserveIfAllowed", refreshDueBranchIndex);
        int verifyCallIndex = source.indexOf("autoCombatPanelService.verifyAndAlignPanel", gateIndex);
        int verifyModeIndex = source.indexOf("AutoCombatPanelService.PanelVerifyMode.VERIFY_AND_REFRESH", verifyCallIndex);

        assertTrue("optional refresh branch must be present", optionalRefreshIndex >= 0);
        assertTrue("refresh-due branch must be present", refreshDueBranchIndex >= 0);
        assertTrue("refresh-due panel verify gate must exist", gateIndex >= 0);
        assertTrue("refresh-and-align call must be present", verifyCallIndex >= 0);
        assertTrue("refresh-and-align mode must be present", verifyModeIndex >= verifyCallIndex);
        assertTrue("refresh-due panel verify gate must run before heavy verify", gateIndex < verifyCallIndex);
        assertContains("refresh-due defer logs before heavy verify",
                source, "refresh-due panel verify deferred by team gate");
        assertContains("per-window retry cooldown is tracked separately from real refresh time",
                source, "lastRefreshDuePanelVerifyAttemptAt");
    }

    private static void entryMaintenanceVerifiesPanelWithoutRoundRefresh() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/bot/dhxy/service/AutoCombatService.java"));
        int methodIndex = service.indexOf("private void maybeRunCombatMaintenance");
        int entryIndex = service.indexOf("auto-combat entry maintenance: clean generic windows and verify panel", methodIndex);
        int optionalRefreshIndex = service.indexOf("// Auto panel refresh is optional", entryIndex);
        String entryBlock = service.substring(entryIndex, optionalRefreshIndex);

        assertContains("entry maintenance must use verify-only mode",
                entryBlock, "AutoCombatPanelService.PanelVerifyMode.ENTRY_MAINTENANCE");
        assertNotContains("entry maintenance must not call default verify-and-refresh",
                entryBlock, "autoCombatPanelService.verifyAndAlignPanel(context)");
        assertNotContains("entry maintenance must not mark a real rounds refresh",
                entryBlock, "state.lastAutoBattleRefreshAt");

        String panel = Files.readString(Path.of("src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java"));
        assertContains("panel verify modes must exist", panel, "enum PanelVerifyMode");
        assertContains("entry maintenance mode must skip round refresh", panel, "ENTRY_MAINTENANCE(false");
        assertContains("periodic mode must keep round refresh", panel, "VERIFY_AND_REFRESH(true");
        assertContains("round refresh is guarded by mode", panel, "if (!safeMode.refreshRounds())");
        assertContains("low/unknown urgent retry guard must be present",
                service, "lastUrgentRoundsPanelVerifyAttemptAt");
    }

    private static void entryMaintenanceDoesNotReturnBeforeOptionalRefresh() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/bot/dhxy/service/AutoCombatService.java"));
        int methodIndex = service.indexOf("private void maybeRunCombatMaintenance");
        int entryIndex = service.indexOf("auto-combat entry maintenance: clean generic windows and verify panel", methodIndex);
        int optionalRefreshIndex = service.indexOf("// Auto panel refresh is optional", entryIndex);
        int earlyReturnIndex = service.indexOf("return;", entryIndex);

        assertTrue("entry maintenance branch must be present", entryIndex >= 0);
        assertTrue("optional refresh branch must follow entry maintenance", optionalRefreshIndex > entryIndex);
        assertTrue("entry maintenance must not return before due/low-round refresh check",
                earlyReturnIndex < 0 || earlyReturnIndex > optionalRefreshIndex);
    }

    private static void entryMaintenanceForcesActualRoundReadBeforeTrustingHealthyCache() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/bot/dhxy/service/AutoCombatService.java"));

        assertContains("entry maintenance must arm one actual round read",
                service, "state.verifyActualRoundsAfterEntryMaintenance = true");
        assertContains("healthy cache branch must honor the armed round read",
                service, "if (state.verifyActualRoundsAfterEntryMaintenance)");
        assertContains("forced actual round read must use VERIFY_AND_REFRESH",
                service, "AutoCombatPanelService.PanelVerifyMode.VERIFY_AND_REFRESH");
        assertContains("forced actual round read must detect real Alt+8 refresh",
                service, "beforeActualRoundReadRefreshAt");
    }

    private static void entryMaintenanceMergesAllowedRefreshDueVerify() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/bot/dhxy/service/AutoCombatService.java"));
        int methodIndex = service.indexOf("private void maybeRunCombatMaintenance");
        int optionalRefreshIndex = service.indexOf("// Auto panel refresh is optional", methodIndex);
        int refreshReasonIndex = service.indexOf("AutoCombatPanelService.RoundsRefreshReason refreshReason", methodIndex);
        int reserveIndex = service.indexOf("refreshDuePanelVerifyGate.reserveIfAllowed", refreshReasonIndex);
        int entryIndex = service.indexOf("auto-combat entry maintenance: clean generic windows and verify panel", methodIndex);
        int mergeLogIndex = service.indexOf("auto-combat entry maintenance: merge panel verify into refresh-due check", entryIndex);
        int entryVerifyIndex = service.indexOf("AutoCombatPanelService.PanelVerifyMode.ENTRY_MAINTENANCE", entryIndex);
        int deferredLogIndex = service.indexOf("refresh-due panel verify deferred by team gate", entryIndex);

        assertTrue("refresh reason should be computed before entry maintenance can duplicate panel verify",
                refreshReasonIndex > methodIndex && refreshReasonIndex < entryIndex);
        assertTrue("refresh-due gate should run before entry maintenance merge decision",
                reserveIndex > refreshReasonIndex && reserveIndex < entryIndex);
        assertTrue("entry maintenance must have a merge log for allowed refresh-due verify",
                mergeLogIndex > entryIndex && mergeLogIndex < entryVerifyIndex);
        assertTrue("entry maintenance verify-only path must still exist after merge branch",
                entryVerifyIndex > mergeLogIndex && entryVerifyIndex < optionalRefreshIndex);
        assertTrue("deferred refresh-due path must still log after entry maintenance",
                deferredLogIndex > entryVerifyIndex);
    }

    private static void verifyAndRefreshReadsVisibleRoundsBeforeTrustingHealthyCache() throws Exception {
        String panel = Files.readString(Path.of("src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java"));
        int refreshMethodIndex = panel.indexOf("private boolean refreshAutoCombatRoundsIfNeeded");
        int readIndex = panel.indexOf("readRemainingRounds(panelMatch, source)", refreshMethodIndex);
        int healthyIndex = panel.indexOf("auto-combat panel rounds estimate healthy after visible/cache check", refreshMethodIndex);

        assertTrue("verify-and-refresh method must exist", refreshMethodIndex >= 0);
        assertTrue("verify-and-refresh must read visible rounds", readIndex > refreshMethodIndex);
        assertTrue("visible rounds must be read before healthy-cache skip", readIndex < healthyIndex);
        assertContains("visible round OCR must update cached estimate",
                panel, "gameContext.setAutoCombatEstimatedRounds(visibleRounds.getAsInt())");
    }

    private static void assertTrue(String label, boolean value) {
        if (!value) {
            throw new AssertionError(label + " expected true");
        }
    }

    private static void assertFalse(String label, boolean value) {
        if (value) {
            throw new AssertionError(label + " expected false");
        }
    }

    private static void assertEquals(String label, Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertContains(String label, String source, String needle) {
        if (!source.contains(needle)) {
            throw new AssertionError(label + " missing: " + needle);
        }
    }

    private static void assertNotContains(String label, String source, String needle) {
        if (source.contains(needle)) {
            throw new AssertionError(label + " unexpected: " + needle);
        }
    }
}
