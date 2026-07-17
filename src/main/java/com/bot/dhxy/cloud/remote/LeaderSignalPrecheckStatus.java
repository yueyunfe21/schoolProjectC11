package com.bot.dhxy.cloud.remote;

/**
 * W-TEAMRETURN-MECH-LEAF-IMP1 (CR TeamReturnService lift-and-shift): byte-faithful migration of HEAD
 * {@code 0114604e} {@code TeamReturnService.LeaderSignalPrecheckStatus}. Field names and factory values
 * are preserved exactly: {@code conclusive/signalPresent/reason}. A non-conclusive status is UNKNOWN
 * (not-ready / stale / failure), never compressed to a false signal.
 */
record LeaderSignalPrecheckStatus(boolean conclusive,
                                  boolean signalPresent,
                                  String reason) {

    static LeaderSignalPrecheckStatus noSignal() {
        return new LeaderSignalPrecheckStatus(true, false, "no-signal");
    }

    static LeaderSignalPrecheckStatus withSignal() {
        return new LeaderSignalPrecheckStatus(true, true, "signal-present");
    }

    static LeaderSignalPrecheckStatus inconclusive(String reason) {
        return new LeaderSignalPrecheckStatus(false, false, reason);
    }
}
