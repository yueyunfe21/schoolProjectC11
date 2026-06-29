package com.bot.dhxy.window.runtime;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.navigation.WorldMapRouteResultPendingMemory;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;

public class WindowRuntimeContextWorldMapMemoryTest {

    public static void main(String[] args) {
        WindowRuntimeContext runtime = new WindowRuntimeContext("test-window", new GameContext());
        runtime.markPathingStarted(WindowPathingIntent.builder()
                .intentId("intent-clear-test")
                .type(WindowPathingIntentType.TARGETED)
                .targetMapName("长安")
                .source("test")
                .createdAtMs(1000L)
                .build());
        WorldMapRouteResultPendingMemory pending = WorldMapRouteResultPendingMemory.builder()
                .fromMap("灵兽村")
                .targetMap("长安")
                .relativeX(11)
                .relativeY(22)
                .source("test-clear")
                .intentId("intent-clear-test")
                .createdAtMs(1001L)
                .build();
        runtime.updatePendingWorldMapRouteResultMemory(pending);

        runtime.clearPathingSignal("test clear");
        WorldMapRouteResultPendingMemory retained = runtime.getPendingWorldMapRouteResultMemory();
        if (retained == null) {
            throw new AssertionError("clearPathingSignal must not silently drop world-map route-result pending memory");
        }
        if (!"intent-clear-test".equals(retained.getIntentId())) {
            throw new AssertionError("unexpected pending intent id: " + retained.getIntentId());
        }
    }
}
