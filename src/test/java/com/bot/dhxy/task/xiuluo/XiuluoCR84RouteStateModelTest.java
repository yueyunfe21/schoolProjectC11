package com.bot.dhxy.task.xiuluo;

/**
 * Focused state-model guard for CR84 修罗 shortcut route metadata.
 */
public class XiuluoCR84RouteStateModelTest {

    public static void main(String[] args) {
        defaultStateKeepsObjectiveRouteCompatibility();
        existingCopyMethodsPreserveShortcutRouteMetadata();
    }

    private static void defaultStateKeepsObjectiveRouteCompatibility() {
        XiuluoRoundContext start = XiuluoRoundContext.start(4);

        require(start.routeMode() == XiuluoRouteMode.OBJECTIVE_NAVIGATION,
                "default route mode must keep the existing objective navigation route");
        require(start.combatSource() == XiuluoCombatSource.NONE,
                "default combat source must not claim shortcut or incidental combat");
        require(start.shortcutTrackerDetailPath() == null, "default state must not carry tracker detail path");
        require(start.shortcutTrackerClickX() == null, "default state must not carry tracker click X");
        require(start.shortcutTrackerClickY() == null, "default state must not carry tracker click Y");
        require(start.firstTrackerGreenClickAtMs() == 0L,
                "default state must not carry tracker green-click timestamp");
        require(start.shortcutTrackerRetryCount() == 0, "default tracker retry count must be zero");
        require(start.shortcutPathingIntentId() == null,
                "default state must not carry an active tracker pathing intent id");
    }

    private static void existingCopyMethodsPreserveShortcutRouteMetadata() {
        XiuluoRoundContext shortcut = XiuluoRoundContext.start(5)
                .withShortcutTrackerClick(
                        XiuluoPhase.WAIT_TRACKER_SHORTCUT_PATHING,
                        "detail.png",
                        new java.awt.Point(320, 456),
                        "tracker-intent-1",
                        "test-shortcut")
                .withCombatSource(XiuluoPhase.WAIT_COMBAT, XiuluoCombatSource.TRACKER_CONFIRM, "test-combat")
                .incrementShortcutTrackerRetry(XiuluoPhase.WAIT_TRACKER_SHORTCUT_PATHING, "test-retry-count")
                .incrementShortcutTrackerRetry(XiuluoPhase.WAIT_TRACKER_SHORTCUT_PATHING, "test-retry-count");

        assertCr84Fields(shortcut.next(XiuluoPhase.READ_OBJECTIVE, "copy-next"));
        assertCr84Fields(shortcut.retrySamePhase("copy-retry"));
        assertCr84Fields(shortcut.waitForPathing("copy-wait"));
        assertCr84Fields(shortcut.clearPathingWait("copy-clear-wait"));
        assertCr84Fields(shortcut.pausePreCombatTimer(1_000L, "copy-pause"));
    }

    private static void assertCr84Fields(XiuluoRoundContext state) {
        require(state.routeMode() == XiuluoRouteMode.TRACKER_SHORTCUT,
                "copy must preserve route mode");
        require(state.combatSource() == XiuluoCombatSource.TRACKER_CONFIRM,
                "copy must preserve combat source");
        require("detail.png".equals(state.shortcutTrackerDetailPath()), "copy must preserve tracker detail path");
        require(Integer.valueOf(320).equals(state.shortcutTrackerClickX()), "copy must preserve tracker click X");
        require(Integer.valueOf(456).equals(state.shortcutTrackerClickY()), "copy must preserve tracker click Y");
        require(state.firstTrackerGreenClickAtMs() > 0L,
                "copy must preserve first tracker green-click timestamp");
        require(state.shortcutTrackerRetryCount() == 2, "copy must preserve tracker retry count");
        require("tracker-intent-1".equals(state.shortcutPathingIntentId()),
                "copy must preserve active tracker pathing intent id");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
