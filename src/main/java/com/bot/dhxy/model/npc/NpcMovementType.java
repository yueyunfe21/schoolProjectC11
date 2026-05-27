package com.bot.dhxy.model.npc;

/**
 * Expected position stability for an NPC target on its map.
 */
public enum NpcMovementType {
    /**
     * The target has a stable logical map coordinate.
     */
    FIXED,

    /**
     * The target can appear at task-provided or changing coordinates.
     */
    ROAMING,

    /**
     * The target normally stays near a known coordinate but may drift within a small area.
     */
    FLOATING,

    /**
     * Position behavior is not known yet.
     */
    UNKNOWN
}
