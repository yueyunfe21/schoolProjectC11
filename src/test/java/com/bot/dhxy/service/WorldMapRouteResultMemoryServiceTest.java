package com.bot.dhxy.service;

import com.bot.dhxy.model.navigation.WorldMapRouteResultMemoryEntry;
import com.bot.dhxy.model.navigation.WorldMapRouteResultPendingMemory;

import java.nio.file.Files;
import java.nio.file.Path;

public class WorldMapRouteResultMemoryServiceTest {

    public static void main(String[] args) throws Exception {
        Path tempDir = Files.createTempDirectory("world-map-route-memory-test");
        Path memoryPath = tempDir.resolve("world_map_route_result_memory.json");
        WorldMapRouteResultMemoryService service = new WorldMapRouteResultMemoryService(memoryPath);

        assertEmpty("blank from map disables lookup", service.findClean(null, "长安").orElse(null));
        assertEmpty("blank target map disables lookup", service.findClean("灵兽村", " ").orElse(null));

        WorldMapRouteResultPendingMemory firstFailure = WorldMapRouteResultPendingMemory.builder()
                .fromMap("长安")
                .targetMap("洛阳")
                .relativeX(123)
                .relativeY(456)
                .matchedText("洛阳(10,20)")
                .source("test:first-failure")
                .usedMemory(false)
                .intentId("intent-first-failure")
                .createdAtMs(500L)
                .build();
        service.recordFailure(firstFailure);
        WorldMapRouteResultMemoryEntry firstFailureEntry = service.findEntry("长安", "洛阳")
                .orElseThrow(() -> new AssertionError("first failure should create dirty entry"));
        assertEquals("first failure relative x", 123, firstFailureEntry.getRelativeX());
        assertEquals("first failure relative y", 456, firstFailureEntry.getRelativeY());
        assertEquals("first failure count", 1, firstFailureEntry.getFailureCount());
        assertEquals("first consecutive failure count", 1, firstFailureEntry.getConsecutiveFailureCount());
        assertEquals("first consecutive success reset", 0, firstFailureEntry.getConsecutiveSuccessCount());
        if (firstFailureEntry.isClean()) {
            throw new AssertionError("first failure entry must stay dirty");
        }
        if (firstFailureEntry.getLastFailureAt() == null || firstFailureEntry.getLastFailureAt().isBlank()) {
            throw new AssertionError("first failure should record failure timestamp");
        }

        WorldMapRouteResultPendingMemory pending = WorldMapRouteResultPendingMemory.builder()
                .fromMap("灵兽村")
                .targetMap("长安")
                .relativeX(444)
                .relativeY(555)
                .matchedText("长安(130,130)")
                .source("test:ocr")
                .usedMemory(false)
                .intentId("intent-1")
                .createdAtMs(1000L)
                .build();

        for (int i = 1; i <= 4; i++) {
            service.recordSuccess(pending.toBuilder().intentId("intent-" + i).build());
            assertEmpty("entry stays dirty before five consecutive successes",
                    service.findClean("灵兽村", "长安").orElse(null));
        }

        service.recordSuccess(pending.toBuilder().intentId("intent-5").build());
        WorldMapRouteResultMemoryEntry clean = service.findClean("灵兽村", "长安")
                .orElseThrow(() -> new AssertionError("entry should become clean after fifth success"));
        assertEquals("relative x persists", 444, clean.getRelativeX());
        assertEquals("relative y persists", 555, clean.getRelativeY());
        assertEquals("success count", 5, clean.getSuccessCount());
        assertEquals("consecutive success count", 5, clean.getConsecutiveSuccessCount());
        assertEquals("consecutive failure count", 0, clean.getConsecutiveFailureCount());

        service.recordFailure(pending.toBuilder().intentId("intent-6").usedMemory(true).build());
        assertEmpty("failure makes entry dirty again", service.findClean("灵兽村", "长安").orElse(null));

        WorldMapRouteResultMemoryService reloaded = new WorldMapRouteResultMemoryService(memoryPath);
        WorldMapRouteResultMemoryEntry dirty = reloaded.findEntry("灵兽村", "长安")
                .orElseThrow(() -> new AssertionError("entry should reload from json"));
        assertEquals("failure count persisted", 1, dirty.getFailureCount());
        assertEquals("success count persisted", 5, dirty.getSuccessCount());
        assertEquals("consecutive success reset", 0, dirty.getConsecutiveSuccessCount());
        assertEquals("consecutive failure incremented", 1, dirty.getConsecutiveFailureCount());

        reloaded.recordAbandoned(pending.toBuilder().intentId("intent-7").build(), "second-navigation");
        WorldMapRouteResultMemoryEntry abandoned = reloaded.findEntry("灵兽村", "长安")
                .orElseThrow(() -> new AssertionError("entry should remain after abandon"));
        assertEquals("abandon does not change success count", 5, abandoned.getSuccessCount());
        assertEquals("abandon does not change failure count", 1, abandoned.getFailureCount());
        if (abandoned.getLastAbandonedAt() == null || abandoned.getLastAbandonedAt().isBlank()) {
            throw new AssertionError("abandon should record metadata timestamp");
        }
    }

    private static void assertEmpty(String caseName, Object value) {
        if (value != null) {
            throw new AssertionError(caseName + ": expected empty but got " + value);
        }
    }

    private static void assertEquals(String caseName, int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError(caseName + ": expected=" + expected + " actual=" + actual);
        }
    }
}
