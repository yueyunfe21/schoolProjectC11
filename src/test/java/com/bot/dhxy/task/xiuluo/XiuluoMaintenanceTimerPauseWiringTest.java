package com.bot.dhxy.task.xiuluo;

/**
 * Source/state guard for CR83 修罗 maintenance pause compensation.
 */
public class XiuluoMaintenanceTimerPauseWiringTest {

    public static void main(String[] args) {
        XiuluoRoundContext start = XiuluoRoundContext.start(3);
        long original = start.preCombatStartedAtMs();
        XiuluoRoundContext paused = start.pausePreCombatTimer(12_000L, "test-maintenance");

        require(paused.preCombatStartedAtMs() == original + 12_000L,
                "maintenance pause must shift the round-local pre-combat watchdog start");
        require(paused.round() == start.round(), "pause must preserve round");
        require(paused.phase() == start.phase(), "pause must preserve phase");
        require(paused.objective() == start.objective(), "pause must preserve objective");
        require(paused.waitingPathing() == start.waitingPathing(), "pause must preserve pathing wait");
        require(paused.enteredBattleByXiuluo() == start.enteredBattleByXiuluo(),
                "pause must preserve battle-entry truth");

        XiuluoRoundContext tiny = start.pausePreCombatTimer(499L, "tiny");
        require(tiny.preCombatStartedAtMs() == original,
                "tiny/non-maintenance pauses below threshold must not shift watchdog budget");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
