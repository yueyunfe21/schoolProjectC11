package com.bot.dhxy.service.npc;

/**
 * Closed set of observable Ctrl-menu probe terminals for NPC smart-click.
 *
 * <p>Each value maps to a HEAD {@code 0114604e} {@code NpcClickService} return branch: the menu tag
 * template did not match (no click), or after a click the verifier confirmed the target, saw an
 * option dialog without confirming the target, or failed verification. A pre-side-effect
 * capture/template failure is not a terminal here: it is reported through the common execution state
 * {@code NOT_EXECUTED} with a null terminal, never fabricated as a business outcome.</p>
 */
public enum CtrlMenuProbeTerminal {
    TAG_NOT_MATCHED,
    VERIFIED,
    DIALOG_OPEN_UNVERIFIED,
    VERIFICATION_FAILED
}
