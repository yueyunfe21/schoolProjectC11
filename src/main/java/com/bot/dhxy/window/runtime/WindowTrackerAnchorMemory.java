package com.bot.dhxy.window.runtime;

/**
 * Window-client-relative center point of the current task-tracker anchor.
 *
 * @param relativeX horizontal pixels from the exact bound window's client origin
 * @param relativeY vertical pixels from the exact bound window's client origin
 */
public record WindowTrackerAnchorMemory(int relativeX, int relativeY) {
}
