package com.bot.dhxy.model.navigation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import com.bot.dhxy.model.MapCoordinate;

/**
 * Snapshot of mini-map label and coordinate recognition.
 *
 * @param mapLabelPath optional debug image path for the cleaned map label.
 * @param coordinate parsed in-game map coordinate, or {@code null} when recognition failed.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Accessors(fluent = true)
public class MiniMapSnapshot {
    String mapLabelPath;
    MapCoordinate coordinate;
}
