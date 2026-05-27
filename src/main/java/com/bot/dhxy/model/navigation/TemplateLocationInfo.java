package com.bot.dhxy.model.navigation;

import com.bot.dhxy.model.MapCoordinate;

/**
 * Fast-path current-location result from mini-map coordinate and map-label templates.
 *
 * @param mapName recognized map name.
 * @param coordinate recognized in-game coordinate.
 * @param mapLabelScore template similarity score for the map label.
 * @param mapLabelPath optional debug image path for the cleaned map label.
 */
public record TemplateLocationInfo(String mapName,
                                   MapCoordinate coordinate,
                                   double mapLabelScore,
                                   String mapLabelPath) {
}
