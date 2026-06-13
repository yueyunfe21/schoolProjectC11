package com.bot.dhxy.window.model;

/**
 * Semantic kind of a window-level pathing observation.
 */
public enum WindowPathingIntentType {
    /**
     * A normal navigation intent with a known map and optionally a known coordinate.
     */
    TARGETED,

    /**
     * A task-tracker green-link click. The game owns the destination, so the watcher can only
     * observe movement/combat/settling; it must not report coordinate arrival.
     */
    UNTARGETED_TRACKER
}
