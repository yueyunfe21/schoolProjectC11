package com.bot.dhxy.model.npc;

/**
 * Business role of an NPC or monster target.
 */
public enum NpcRole {
    /**
     * NPC that starts, submits, or advances a task.
     */
    QUEST_GIVER,

    /**
     * Monster or enemy that should be clicked to enter combat.
     */
    COMBAT_TARGET,

    /**
     * Non-combat NPC used for travel, dialog, shop, or other interaction.
     */
    INTERACTION_TARGET,

    /**
     * Target used only by debug/calibration tools.
     */
    DEBUG_TARGET
}
