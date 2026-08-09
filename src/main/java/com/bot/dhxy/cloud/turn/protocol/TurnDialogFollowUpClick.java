package com.bot.dhxy.cloud.turn.protocol;

/**
 * Optional second click carried by an existing dialog-interest update.
 *
 * @param absoluteX screen-absolute X coordinate prepared from the same Tracker frame.
 * @param absoluteY screen-absolute Y coordinate prepared from the same Tracker frame.
 * @param pathingIntent exact pathing intent to register after both local clicks complete.
 */
public record TurnDialogFollowUpClick(
        int absoluteX,
        int absoluteY,
        TurnPathingIntent pathingIntent) {
}
