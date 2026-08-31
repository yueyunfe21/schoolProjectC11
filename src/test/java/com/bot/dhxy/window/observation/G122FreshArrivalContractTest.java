package com.bot.dhxy.window.observation;

import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.model.WindowPathingState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G122 P1-1 contracts: a fresh recognized coordinate of the SAME exact intent that already sits
 * inside the target tolerance proves arrival by itself. Pinned against the real production decision
 * {@link WindowObservationSampler#freshCoordinateProvesArrival} with the 2026-08-29 11:40 incident
 * values: target 洛阳城 (225,222) tolerance 5, fresh reads (225,223)/(224,221) repeatedly in
 * tolerance, yet no ARRIVED for 67 s because the walking-baseline branch never classified and the
 * terminal gate demanded matching request stamps that pixel-strip noise kept resetting.
 */
class G122FreshArrivalContractTest {

    private static final String MAP = "洛阳城";

    private static WindowPathingIntent incidentIntent() {
        return WindowPathingIntent.builder()
                .intentId("g122-intent")
                .targetMapName(MAP)
                .targetX(225)
                .targetY(222)
                .tolerance(5)
                .type(WindowPathingIntentType.TARGETED)
                .build();
    }

    /** Contract gate (1)/(2): the two real incident coordinates must prove arrival, no movement needed. */
    @Test
    void bothIncidentCoordinatesProveArrival() {
        WindowPathingIntent intent = incidentIntent();
        assertTrue(WindowObservationSampler.freshCoordinateProvesArrival(
                intent, WindowPathingState.ACTIVE, "g122-intent", MAP, 225, 223));
        assertTrue(WindowObservationSampler.freshCoordinateProvesArrival(
                intent, WindowPathingState.ACTIVE, "g122-intent", MAP, 224, 221));
    }

    /** Contract gate (1): starting already inside tolerance with zero movement still arrives. */
    @Test
    void startingInsideToleranceStillArrives() {
        assertTrue(WindowObservationSampler.freshCoordinateProvesArrival(
                incidentIntent(), WindowPathingState.UNKNOWN, "g122-intent", MAP, 225, 222));
    }

    /** Fail-closed: an old intent's coordinate must never arrive the current leg. */
    @Test
    void oldIntentFailsClosed() {
        assertFalse(WindowObservationSampler.freshCoordinateProvesArrival(
                incidentIntent(), WindowPathingState.ACTIVE, "some-older-intent", MAP, 225, 222));
        assertFalse(WindowObservationSampler.freshCoordinateProvesArrival(
                null, WindowPathingState.ACTIVE, "g122-intent", MAP, 225, 222));
    }

    /** Fail-closed: a coordinate on the wrong map proves nothing. */
    @Test
    void wrongMapFailsClosed() {
        assertFalse(WindowObservationSampler.freshCoordinateProvesArrival(
                incidentIntent(), WindowPathingState.ACTIVE, "g122-intent", "长安城", 225, 222));
    }

    /** Fail-closed: outside tolerance falls back to the legacy classification chain. */
    @Test
    void outsideToleranceFailsClosed() {
        assertFalse(WindowObservationSampler.freshCoordinateProvesArrival(
                incidentIntent(), WindowPathingState.ACTIVE, "g122-intent", MAP, 231, 222));
        assertFalse(WindowObservationSampler.freshCoordinateProvesArrival(
                incidentIntent(), WindowPathingState.ACTIVE, "g122-intent", MAP, 225, 228));
    }

    /** Fail-closed: a leg that already reached a terminal must not be re-arrived by a late frame. */
    @Test
    void terminalLegFailsClosed() {
        assertFalse(WindowObservationSampler.freshCoordinateProvesArrival(
                incidentIntent(), WindowPathingState.ARRIVED, "g122-intent", MAP, 225, 222));
        assertFalse(WindowObservationSampler.freshCoordinateProvesArrival(
                incidentIntent(), WindowPathingState.STOPPED_AWAY, "g122-intent", MAP, 225, 222));
    }

    /** Fail-closed: untargeted tracker legs have no coordinate target to prove. */
    @Test
    void untargetedTrackerLegFailsClosed() {
        WindowPathingIntent untargeted = incidentIntent().toBuilder()
                .type(WindowPathingIntentType.UNTARGETED_TRACKER)
                .build();
        assertFalse(WindowObservationSampler.freshCoordinateProvesArrival(
                untargeted, WindowPathingState.ACTIVE, "g122-intent", MAP, 225, 222));
    }

    /** Fail-closed: a coordinate-less intent cannot use the fresh-coordinate arrival proof. */
    @Test
    void coordinatelessIntentFailsClosed() {
        WindowPathingIntent mapOnly = incidentIntent().toBuilder().targetX(null).targetY(null).build();
        assertFalse(WindowObservationSampler.freshCoordinateProvesArrival(
                mapOnly, WindowPathingState.ACTIVE, "g122-intent", MAP, 225, 222));
    }
}
