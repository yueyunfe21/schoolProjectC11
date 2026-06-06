package com.bot.dhxy.model.npc;

/**
 * Visual tooltip category expected above an NPC or task target.
 */
public enum NpcTooltipType {
    /**
     * The target can show the standard task tooltip image above its head.
     */
    TASK,

    /**
     * Super witch doctor / heal-pet NPC tooltip.
     */
    WUYI,

    /**
     * The target is not expected to show a clickable task tooltip, so template probing should skip it.
     */
    NONE
}
