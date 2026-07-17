package com.bot.dhxy.service.npc;

/**
 * Closed set of Ctrl-menu scan-rectangle kinds for NPC smart-click probes.
 *
 * <p>The only kind is the probe-centred ring rectangle derived by the single HEAD
 * {@code 0114604e} {@code NpcClickService.buildCtrlMenuScanRect} formula (half-width/height
 * constants clamped to the bound window). The concrete rectangle is resolved by the local handler
 * that owns the exact window binding; no free rectangle is ever transmitted.</p>
 */
public enum CtrlScanRectKind {
    CTRL_MENU_PROBE_RING
}
