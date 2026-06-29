package com.bot.dhxy.service;

import com.bot.dhxy.model.navigation.WorldMapRouteResultMemoryEntry;
import com.bot.dhxy.model.navigation.WorldMapRouteResultMode;
import com.bot.dhxy.model.navigation.WorldMapRouteResultPendingMemory;

import java.nio.file.Files;
import java.nio.file.Path;

public class WorldMapRouteResultYellowMemoryModeGuard {

    public static void main(String[] args) throws Exception {
        Path memoryPath = Files.createTempDirectory("world-map-yellow-memory-guard")
                .resolve("world_map_route_result_memory.json");
        WorldMapRouteResultMemoryService service = new WorldMapRouteResultMemoryService(memoryPath);

        WorldMapRouteResultPendingMemory legacyPending = pending(null, 111, 222, "legacy-green");
        for (int i = 1; i <= 5; i++) {
            service.recordSuccess(legacyPending.toBuilder().intentId("legacy-" + i).build());
        }

        requirePresent("legacy/null-mode entry should remain usable as legacy green",
                service.findClean("长安", "洛阳", WorldMapRouteResultMode.LEGACY_GREEN_LINK).orElse(null));
        requireAbsent("legacy/null-mode entry must not be reused as yellow destination memory",
                service.findClean("长安", "洛阳", WorldMapRouteResultMode.YELLOW_DESTINATION_MINI_MAP).orElse(null));

        WorldMapRouteResultPendingMemory yellowPending = pending(
                WorldMapRouteResultMode.YELLOW_DESTINATION_MINI_MAP, 333, 444, "yellow-destination");
        for (int i = 1; i <= 5; i++) {
            service.recordSuccess(yellowPending.toBuilder().intentId("yellow-" + i).build());
        }

        WorldMapRouteResultMemoryEntry yellow = service
                .findClean("长安", "洛阳", WorldMapRouteResultMode.YELLOW_DESTINATION_MINI_MAP)
                .orElseThrow(() -> new AssertionError("yellow memory should become clean after five successes"));
        assertEquals("yellow relative x", 333, yellow.getRelativeX());
        assertEquals("yellow relative y", 444, yellow.getRelativeY());
        if (yellow.getRouteMode() != WorldMapRouteResultMode.YELLOW_DESTINATION_MINI_MAP) {
            throw new AssertionError("yellow entry must persist yellow routeMode, actual=" + yellow.getRouteMode());
        }

        service.recordFailure(yellowPending.toBuilder().intentId("yellow-failure").usedMemory(true).build());
        requireAbsent("yellow failure should dirty only yellow entry",
                service.findClean("长安", "洛阳", WorldMapRouteResultMode.YELLOW_DESTINATION_MINI_MAP).orElse(null));
        requirePresent("yellow failure must not dirty legacy green entry",
                service.findClean("长安", "洛阳", WorldMapRouteResultMode.LEGACY_GREEN_LINK).orElse(null));
    }

    private static WorldMapRouteResultPendingMemory pending(WorldMapRouteResultMode mode,
                                                           int relativeX,
                                                           int relativeY,
                                                           String source) {
        return WorldMapRouteResultPendingMemory.builder()
                .fromMap("长安")
                .targetMap("洛阳")
                .routeMode(mode)
                .relativeX(relativeX)
                .relativeY(relativeY)
                .matchedText(source)
                .source("guard:" + source)
                .intentId("intent-" + source)
                .createdAtMs(System.currentTimeMillis())
                .build();
    }

    private static void requirePresent(String caseName, Object value) {
        if (value == null) {
            throw new AssertionError(caseName + ": expected present");
        }
    }

    private static void requireAbsent(String caseName, Object value) {
        if (value != null) {
            throw new AssertionError(caseName + ": expected absent but got " + value);
        }
    }

    private static void assertEquals(String caseName, int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError(caseName + ": expected=" + expected + " actual=" + actual);
        }
    }
}
