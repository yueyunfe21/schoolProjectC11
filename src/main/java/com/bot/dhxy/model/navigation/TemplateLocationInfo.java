package com.bot.dhxy.model.navigation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import com.bot.dhxy.model.MapCoordinate;

/**
 * Fast-path current-location result from mini-map coordinate and map-label templates.
 *
 * @param mapName recognized map name.
 * @param coordinate recognized in-game coordinate.
 * @param mapLabelScore template similarity score for the map label.
 * @param mapLabelPath optional debug image path for the cleaned map label; null on normal in-memory fast paths.
 * @param ocrFallback CR246: true when the cloud produced this result through its OCR fallback
 *                    instead of a template hit; consumers apply the old OCR discipline
 *                    (canonicalization + coordinate plausibility) to such results.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Accessors(fluent = true)
public class TemplateLocationInfo {
    String mapName;
    MapCoordinate coordinate;
    double mapLabelScore;
    String mapLabelPath;
    boolean ocrFallback;
}
