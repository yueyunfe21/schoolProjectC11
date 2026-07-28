package com.bot.dhxy.window.observation;

import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.model.WindowPathingState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WindowObservationPathingPolicyTest {

    private static final WindowPathingIntent TARGETED = WindowPathingIntent.builder()
            .intentId("intent-cr142")
            .source("test:cr142")
            .targetMapName("灵兽村")
            .targetX(115)
            .targetY(71)
            .tolerance(12)
            .type(WindowPathingIntentType.TARGETED)
            .createdAtMs(1L)
            .build();
    private static final WindowPathingIntent UNTARGETED_TRACKER = WindowPathingIntent.builder()
            .intentId("intent-tracker")
            .source("test:tracker-green")
            .type(WindowPathingIntentType.UNTARGETED_TRACKER)
            .createdAtMs(1L)
            .build();

    @Test
    void targetedCoordinateRequiresCr142StationaryWindowBeforeArrival() {
        assertEquals(WindowPathingState.ACTIVE,
                classify("灵兽村", 113, 73, 599L, 5_000L, true, 0L));
        assertEquals(WindowPathingState.ARRIVED,
                classify("灵兽村", 113, 73, 600L, 5_000L, true, 0L));
    }

    @Test
    void firstOrChangedCoordinateCannotPublishStoppedAway() {
        assertEquals(WindowPathingState.ACTIVE,
                classify("灵兽村", 90, 40, 5_000L, 5_000L, true, 0L));
    }

    @Test
    void unchangedRecognizedCoordinateUsesBaselineStoppedAwayBoundary() {
        assertEquals(WindowPathingState.ACTIVE,
                classify("灵兽村", 90, 40, 5_000L, 5_000L, false, 2_199L));
        assertEquals(WindowPathingState.STOPPED_AWAY,
                classify("灵兽村", 90, 40, 5_000L, 5_000L, false, 2_200L));
    }

    @Test
    void untargetedTrackerRequiresTwoRecognizedLocationsBeforeStoppedAway() {
        assertEquals(WindowPathingState.ACTIVE,
                classify(UNTARGETED_TRACKER, "大雁塔一层", 128, 82,
                        2_551L, 20_000L, true, 0L));
        assertEquals(WindowPathingState.ACTIVE,
                classify(UNTARGETED_TRACKER, "大雁塔一层", 66, 82,
                        5_000L, 24_000L, true, 0L));
        assertEquals(WindowPathingState.STOPPED_AWAY,
                classify(UNTARGETED_TRACKER, "大雁塔一层", 66, 82,
                        7_500L, 26_500L, false, 2_500L));
    }

    @Test
    void freshIntentCannotPublishTerminalFromAnOldStationaryFrame() {
        assertEquals(WindowPathingState.ACTIVE,
                classify("灵兽村", 113, 73, 5_000L, 599L, false, 5_000L));
        assertEquals(WindowPathingState.ACTIVE,
                classify("灵兽村", 90, 40, 5_000L, 2_199L, false, 5_000L));
    }

    private static WindowPathingState classify(String mapName,
                                                int x,
                                                int y,
                                                long stableMs,
                                                long intentAgeMs,
                                                boolean recognizedLocationChanged,
                                                long recognizedStationaryMs) {
        return classify(TARGETED, mapName, x, y, stableMs, intentAgeMs,
                recognizedLocationChanged, recognizedStationaryMs);
    }

    private static WindowPathingState classify(WindowPathingIntent intent,
                                                String mapName,
                                                int x,
                                                int y,
                                                long stableMs,
                                                long intentAgeMs,
                                                boolean recognizedLocationChanged,
                                                long recognizedStationaryMs) {
        return WindowObservationSampler.classifyRecognizedPathingState(
                intent, mapName, x, y, stableMs, intentAgeMs,
                recognizedLocationChanged, recognizedStationaryMs);
    }
}
