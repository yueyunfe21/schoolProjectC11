package com.bot.dhxy.service;

public class AutoCombatPanelRefreshDueBurstGuardTest {

    public static void main(String[] args) {
        refreshDueDefersInsideSameTeamGuardWindow();
        refreshDueAllowsDifferentTeamAndAfterGuardWindow();
        lowRoundsAndUnknownBypassRefreshDueGuard();
    }

    private static void refreshDueDefersInsideSameTeamGuardWindow() {
        AutoCombatPanelService.TeamRefreshDueBurstGuard guard =
                new AutoCombatPanelService.TeamRefreshDueBurstGuard();

        AutoCombatPanelService.RefreshDueBurstDecision first =
                guard.reserveIfAllowed("wubei", "hwnd-A", "refresh-due", 1_000L);
        AutoCombatPanelService.RefreshDueBurstDecision second =
                guard.reserveIfAllowed("wubei", "hwnd-B", "refresh-due", 20_000L);

        assertFalse("first same-team refresh-due is allowed", first.deferred());
        assertTrue("second same-team refresh-due inside 30s is deferred", second.deferred());
        assertEquals("retry after", 11_000L, second.retryAfterMs());
        assertEquals("last age", 19_000L, second.lastTeamRefreshAgeMs());
    }

    private static void refreshDueAllowsDifferentTeamAndAfterGuardWindow() {
        AutoCombatPanelService.TeamRefreshDueBurstGuard guard =
                new AutoCombatPanelService.TeamRefreshDueBurstGuard();

        guard.reserveIfAllowed("wubei", "hwnd-A", "refresh-due", 1_000L);
        AutoCombatPanelService.RefreshDueBurstDecision differentTeam =
                guard.reserveIfAllowed("xiuluo_v2", "hwnd-B", "refresh-due", 2_000L);
        AutoCombatPanelService.RefreshDueBurstDecision afterWindow =
                guard.reserveIfAllowed("wubei", "hwnd-C", "refresh-due", 31_000L);

        assertFalse("different team refresh-due is independent", differentTeam.deferred());
        assertFalse("same team refresh-due after 30s is allowed", afterWindow.deferred());
    }

    private static void lowRoundsAndUnknownBypassRefreshDueGuard() {
        AutoCombatPanelService.TeamRefreshDueBurstGuard guard =
                new AutoCombatPanelService.TeamRefreshDueBurstGuard();

        guard.reserveIfAllowed("wubei", "hwnd-A", "refresh-due", 1_000L);

        assertFalse("low-rounds bypasses refresh-due guard",
                guard.reserveIfAllowed("wubei", "hwnd-B", "low-rounds", 2_000L).deferred());
        assertFalse("unknown bypasses refresh-due guard",
                guard.reserveIfAllowed("wubei", "hwnd-C", "unknown", 3_000L).deferred());
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

    private static void assertEquals(String label, long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }
}
