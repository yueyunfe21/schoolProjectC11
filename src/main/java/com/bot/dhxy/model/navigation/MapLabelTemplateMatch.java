package com.bot.dhxy.model.navigation;

/**
 * Candidate map-name template match from the mini-map label strip.
 *
 * @param mapName template map name.
 * @param score similarity score; callers decide their own acceptance threshold.
 */
public record MapLabelTemplateMatch(String mapName, double score) {
}
