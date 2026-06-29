package com.bot.dhxy.window.runtime;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.task.model.TaskType;

/**
 * State guard for CR83 ordinary 五倍 pre-battle timer compensation.
 */
public class WindowRuntimeOrdinaryPreBattlePauseTest {

    public static void main(String[] args) {
        WindowRuntimeContext runtime = new WindowRuntimeContext("window-cr83", new GameContext());
        runtime.startOrdinaryPreBattleTimer(TaskType.WUBEI, "test-green", "长安", 1_000L);
        runtime.startOrdinaryEnterBattleTargetMapGate(TaskType.WUBEI, "test-green", "长安", 2_000L);
        runtime.markOrdinaryEnterBattleTargetMapGateOpened(3_000L);

        boolean shifted = runtime.pauseOrdinaryPreBattleTimer(12_000L, "cr83-test");
        require(shifted, "active ordinary timer must be shifted");
        require(runtime.getOrdinaryPreBattleStartedAtMs() == 13_000L,
                "ordinary pre-battle start must shift by blocked duration");
        require(runtime.getOrdinaryEnterBattleTargetMapGateStartedAtMs() == 14_000L,
                "ordinary target-map gate start must shift by blocked duration");
        require(runtime.getOrdinaryEnterBattleTargetMapOpenedAtMs() == 15_000L,
                "ordinary target-map opened time must shift by blocked duration");

        runtime.clearOrdinaryPreBattleTimer("test-clear");
        require(!runtime.pauseOrdinaryPreBattleTimer(12_000L, "inactive"),
                "inactive ordinary timer must not report a shift");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
