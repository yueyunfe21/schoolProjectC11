package com.bot.dhxy.cloud.remote;

/**
 * W-TEAMRETURN-MECH-LEAF-IMP1 (CR TeamReturnService lift-and-shift): byte-faithful migration of HEAD
 * {@code 0114604e} {@code TeamReturnService.LeaderSignalPrecheckResult}. The non-referenceable private
 * nested record is reproduced verbatim in the trusted remote package as the registry's typed
 * {@code R}. All factory field values match the baseline exactly.
 */
record LeaderSignalPrecheckResult(LeaderSignalPrecheckResultStatus status,
                                  int absoluteX,
                                  int absoluteY,
                                  String reason) {

    static LeaderSignalPrecheckResult noSignal() {
        return new LeaderSignalPrecheckResult(LeaderSignalPrecheckResultStatus.NO_SIGNAL, -1, -1, "no-signal");
    }

    static LeaderSignalPrecheckResult signalPresent(int absoluteX, int absoluteY) {
        return new LeaderSignalPrecheckResult(LeaderSignalPrecheckResultStatus.SIGNAL_PRESENT,
                absoluteX, absoluteY, "signal-present");
    }

    static LeaderSignalPrecheckResult failed(String reason) {
        return new LeaderSignalPrecheckResult(LeaderSignalPrecheckResultStatus.FAILED, -1, -1, reason);
    }
}
