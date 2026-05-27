package com.bot.dhxy.model.navigation;

/**
 * Parsed destination from a task objective panel or story dialog.
 *
 * @param mapSlug normalized map id used by templates/config.
 * @param mapName recognized display map name.
 * @param x in-game map X coordinate.
 * @param y in-game map Y coordinate.
 * @param mapScore template similarity score for the map name.
 * @param source diagnostic source path/stage.
 */
public record ObjectiveTextResult(String mapSlug,
                                  String mapName,
                                  int x,
                                  int y,
                                  double mapScore,
                                  String source) {
}
