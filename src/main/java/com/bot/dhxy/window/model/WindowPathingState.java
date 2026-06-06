package com.bot.dhxy.window.model;

/**
 * Window-level observation state for a navigation/pathing handoff.
 */
public enum WindowPathingState {
    NONE,
    ACTIVE,
    ARRIVED,
    STOPPED_AWAY,
    UNKNOWN
}
