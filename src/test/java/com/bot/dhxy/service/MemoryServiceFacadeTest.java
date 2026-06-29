package com.bot.dhxy.service;

import com.bot.dhxy.model.navigation.WorldMapRouteResultMemoryEntry;
import com.bot.dhxy.model.navigation.WorldMapRouteResultPendingMemory;

import java.nio.file.Files;
import java.nio.file.Path;

public class MemoryServiceFacadeTest {

    public static void main(String[] args) throws Exception {
        Path tempDir = Files.createTempDirectory("dhxy-memory-facade-test");
        MemoryService memoryService = new MemoryService(
                new DialogChoiceMemoryService(tempDir.resolve("dialog_choice_memory.json")),
                new WorldMapRouteResultMemoryService(tempDir.resolve("world_map_route_result_memory.json")));

        memoryService.recordDialogChoiceSuccess(
                "wubei", "acceptTask", "降魔", "宝象国", 86, 87, "宝象国",
                12, 34, "除魔卫", "facade-test");
        MemoryService.DialogChoiceEntry dialogEntry = memoryService
                .findUsableDialogChoice("wubei", "acceptTask", "降魔")
                .orElseThrow(() -> new AssertionError("dialog choice memory should be available through facade"));
        assertEquals("dialog relative X", 12, dialogEntry.getRelativeX());
        assertEquals("dialog relative Y", 34, dialogEntry.getRelativeY());
        assertAbsent("stable task choice should require three consecutive successes",
                memoryService.findStableTaskDialogChoice("wubei", "acceptTask", "降魔"));
        memoryService.recordDialogChoiceSuccess(
                "wubei", "acceptTask", "降魔", "宝象国", 86, 87, "宝象国",
                12, 34, "除魔卫", "facade-test");
        memoryService.recordDialogChoiceFailure("wubei", "acceptTask", "降魔", "facade-test");
        memoryService.recordDialogChoiceSuccess(
                "wubei", "acceptTask", "降魔", "宝象国", 86, 87, "宝象国",
                12, 34, "除魔卫", "facade-test");
        memoryService.recordDialogChoiceSuccess(
                "wubei", "acceptTask", "降魔", "宝象国", 86, 87, "宝象国",
                12, 34, "除魔卫", "facade-test");
        assertAbsent("failure should reset task choice consecutive success streak",
                memoryService.findStableTaskDialogChoice("wubei", "acceptTask", "降魔"));
        memoryService.recordDialogChoiceSuccess(
                "wubei", "acceptTask", "降魔", "宝象国", 86, 87, "宝象国",
                12, 34, "除魔卫", "facade-test");
        MemoryService.DialogChoiceEntry stableDialogEntry = memoryService
                .findStableTaskDialogChoice("wubei", "acceptTask", "降魔")
                .orElseThrow(() -> new AssertionError("stable task choice should be available after three consecutive successes"));
        assertEquals("stable dialog consecutive success count", 3, stableDialogEntry.getConsecutiveSuccessCount());

        WorldMapRouteResultPendingMemory pending = WorldMapRouteResultPendingMemory.builder()
                .fromMap("灵兽村")
                .targetMap("长安")
                .relativeX(410)
                .relativeY(220)
                .matchedText("长安")
                .source("facade-test")
                .usedMemory(false)
                .intentId("intent-1")
                .build();
        for (int i = 0; i < 5; i++) {
            memoryService.recordWorldMapRouteResultSuccess(pending.toBuilder().intentId("intent-" + i).build());
        }
        WorldMapRouteResultMemoryEntry routeEntry = memoryService
                .findCleanWorldMapRouteResult("灵兽村", "长安")
                .orElseThrow(() -> new AssertionError("world-map route memory should be available through facade"));
        assertEquals("route relative X", 410, routeEntry.getRelativeX());
        assertEquals("route relative Y", 220, routeEntry.getRelativeY());
        assertEquals("route success count", 5, routeEntry.getSuccessCount());
    }

    private static void assertEquals(String label, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertAbsent(String label, java.util.Optional<?> value) {
        if (value.isPresent()) {
            throw new AssertionError(label + " expected empty but was present");
        }
    }
}
