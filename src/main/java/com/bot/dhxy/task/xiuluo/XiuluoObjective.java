package com.bot.dhxy.task.xiuluo;

/**
 * Parsed Xiuluo target objective.
 *
 * @param mapName game-visible target map name.
 * @param x logical in-game target X coordinate.
 * @param y logical in-game target Y coordinate.
 * @param rawText original story/task-panel text or template source used to create the objective.
 */
public record XiuluoObjective(String mapName, int x, int y, String rawText) {
}
