package com.bot.dhxy.service.npc;

/**
 * Closed set of local post-click verification results for NPC smart-click.
 *
 * <p>Values map to HEAD {@code 0114604e} {@code NpcClickService} verification branches: the target
 * was verified, an option dialog was visible without confirming the target, or nothing expected was
 * visible. Present only when the common execution state is {@code OBSERVED}; the
 * defer-to-task verification path does not issue this remote verify and is not represented here.</p>
 */
public enum NpcLocalVerifyResult {
    VERIFIED,
    DIALOG_OPEN_UNVERIFIED,
    NOT_VISIBLE
}
