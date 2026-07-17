package com.bot.dhxy.cloud.remote;

/**
 * W-TEAMRETURN-MECH-LEAF-IMP1 (CR TeamReturnService lift-and-shift): byte-faithful migration of HEAD
 * {@code 0114604e} {@code TeamReturnService.LeaderSignalPrecheckResult}'s status. {@code NO_SIGNAL} is an
 * explicit negative observation, not a compressed false; UNKNOWN/STOPPED are never folded into it.
 */
enum LeaderSignalPrecheckResultStatus {
    NO_SIGNAL,
    SIGNAL_PRESENT,
    FAILED
}
