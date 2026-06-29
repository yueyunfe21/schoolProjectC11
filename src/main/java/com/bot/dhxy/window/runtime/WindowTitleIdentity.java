package com.bot.dhxy.window.runtime;

/**
 * Player identity parsed from a live DHXY game-window title.
 *
 * @param server game server name parsed from the title.
 * @param playerName character name parsed from the title.
 * @param playerId numeric character id parsed from the title.
 */
public record WindowTitleIdentity(String server, String playerName, String playerId) {

    public boolean samePlayer(WindowTitleIdentity other) {
        return other != null
                && equalsText(server, other.server)
                && equalsText(playerName, other.playerName)
                && equalsText(playerId, other.playerId);
    }

    private static boolean equalsText(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
