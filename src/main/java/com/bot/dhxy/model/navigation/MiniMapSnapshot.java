package com.bot.dhxy.model.navigation;

import com.bot.dhxy.model.MapCoordinate;

/**
 * Snapshot of mini-map label and coordinate recognition.
 *
 * @param mapLabelPath optional debug image path for the cleaned map label.
 * @param coordinate parsed in-game map coordinate, or {@code null} when recognition failed.
 */
public record MiniMapSnapshot(String mapLabelPath, MapCoordinate coordinate) {
}
