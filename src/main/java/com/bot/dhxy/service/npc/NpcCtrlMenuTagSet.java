package com.bot.dhxy.service.npc;

/**
 * Closed set of Ctrl-menu tag template matchers for NPC smart-click probes.
 *
 * <p>The only member is the clean NPC-menu tag template match from HEAD {@code 0114604e}
 * {@code NpcClickService} (the {@code npc_menu_clean_sample.png} template with its fixed match
 * threshold). This is a single template matcher, not a keyword string list; the template resource
 * and threshold stay local and are never transmitted as raw bytes.</p>
 */
public enum NpcCtrlMenuTagSet {
    NPC_MENU_CLEAN_TAG
}
