package com.bot.dhxy.window.runtime;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR95 player-scoped cache invalidation wiring.
 */
public class WindowIdentityDriftCacheInvalidationWiringTest {

    public static void main(String[] args) throws Exception {
        Path maintenanceSource = Path.of("src", "main", "java", "com", "bot", "dhxy", "service",
                "TaskMaintenanceService.java");
        Path playerStateSource = Path.of("src", "main", "java", "com", "bot", "dhxy", "service",
                "PlayerStateService.java");
        String maintenance = Files.readString(maintenanceSource, StandardCharsets.UTF_8);
        String playerState = Files.readString(playerStateSource, StandardCharsets.UTF_8);

        assertContains(maintenance, "currentPlayerIdentityEpoch()");
        assertContains(maintenance, "existing.playerIdentityEpoch != epoch");
        assertContains(maintenance, "invalidate summon skill cache by player identity drift");
        assertContains(maintenance, "lastSummonSkillCleanAtByWindow.remove(windowKey)");
        assertContains(maintenance, "private long playerIdentityEpoch;");

        assertContains(playerState, "getPlayerIdentityEpoch()");
        assertContains(playerState, "existing.playerIdentityEpoch != epoch");
        assertContains(playerState, "player-state runtime cache invalidated by player identity drift");
        assertContains(playerState, "private long playerIdentityEpoch;");

        System.out.println("WindowIdentityDriftCacheInvalidationWiringTest passed");
    }

    private static void assertContains(String value, String token) {
        if (!value.contains(token)) {
            throw new AssertionError("Expected token missing: " + token);
        }
    }
}
