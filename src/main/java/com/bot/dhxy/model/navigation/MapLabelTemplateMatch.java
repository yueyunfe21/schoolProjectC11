package com.bot.dhxy.model.navigation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Candidate map-name template match from the mini-map label strip.
 *
 * @param mapName template map name.
 * @param score similarity score; callers decide their own acceptance threshold.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Accessors(fluent = true)
public class MapLabelTemplateMatch {
    String mapName;
    double score;
}
