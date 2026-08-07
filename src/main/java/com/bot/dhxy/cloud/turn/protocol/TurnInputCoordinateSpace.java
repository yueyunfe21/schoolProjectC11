package com.bot.dhxy.cloud.turn.protocol;

/** Coordinate basis carried by one typed mouse-input step. */
public enum TurnInputCoordinateSpace {
    /** Coordinates are immutable physical screen pixels, retained for every legacy input step. */
    SCREEN_ABSOLUTE,
    /** Coordinates are pixels relative to the exact bound HWND and are projected only at local execution time. */
    WINDOW_RELATIVE
}
