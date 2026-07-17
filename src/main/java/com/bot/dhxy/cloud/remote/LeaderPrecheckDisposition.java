package com.bot.dhxy.cloud.remote;

/**
 * W-TEAMRETURN-TYPES-IMP1 (CR TeamReturnService lift-and-shift): closed business disposition of one
 * leader-precheck observation, covering the baseline signal / no-signal outcomes and every mechanical
 * inconclusive branch. Only {@code SIGNAL_PRESENT} / {@code NO_SIGNAL} are conclusive; the rest are
 * inconclusive (live fallback). The Cloud mirror
 * {@code com.yueyunfe.dhxy.cloudbrain.remote.LeaderPrecheckDisposition} keeps the same names and order.
 */
public enum LeaderPrecheckDisposition {
    SIGNAL_PRESENT,
    NO_SIGNAL,
    CAPTURE_FAILED,
    ATTACH_FAILED,
    SUBMIT_REJECTED,
    ANALYSIS_FAILED,
    NOT_READY,
    STALE,
    REUSED_ACTIVE,
    TEARDOWN_BUSY,
    CAPACITY_REJECTED
}
