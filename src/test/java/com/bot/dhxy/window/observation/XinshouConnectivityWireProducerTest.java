package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.protocol.observation.ObservationInterest;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationKeyEvent;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationKeyEventType;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationProtocolValidator;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationRequest;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Produces the Client half of the cross-repository connectivity fixture through the real
 * {@link WindowObservationRunner}. The Cloud phase consumes the resulting strict wire JSON.
 */
class XinshouConnectivityWireProducerTest {

    private static final String TENANT = "tenant";
    private static final String DEVICE = "device";
    private static final String WINDOW = "window";
    private static final String HWND = "0x40f";
    private static final String TASK_RUN = "xinshou-connectivity";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void runnerSerializesExactCombatEdgesForCloudConnectivityPhase() throws Exception {
        Path output = connectivityDir();
        Files.createDirectories(output);
        ArtifactObservationClient client = new ArtifactObservationClient(output);
        WindowObservationRunner runner = new WindowObservationRunner(
                client,
                TENANT,
                DEVICE,
                WINDOW,
                HWND,
                "xinshou",
                TASK_RUN,
                null,
                25L);
        try {
            runner.start();
            assertTrue(client.initialRequest.await(3, TimeUnit.SECONDS),
                    "runner did not establish the observation wire");

            long enteredAtMs = System.currentTimeMillis() - 5_100L;
            runner.publishKeyEvent(combatEvent(
                    "xinshou-connectivity-enter",
                    ObservationKeyEventType.IN_COMBAT,
                    enteredAtMs,
                    1L));
            assertTrue(client.enterRequest.await(3, TimeUnit.SECONDS),
                    "IN_COMBAT did not cross the Client observation wire");

            runner.publishKeyEvent(combatEvent(
                    "xinshou-connectivity-exit",
                    ObservationKeyEventType.COMBAT_EXITED,
                    System.currentTimeMillis(),
                    1L));
            assertTrue(client.exitRequest.await(3, TimeUnit.SECONDS),
                    "COMBAT_EXITED did not cross the Client observation wire");
        } finally {
            runner.requestStop();
            assertTrue(runner.awaitStopped(Duration.ofSeconds(3)));
        }
    }

    private static ObservationKeyEvent combatEvent(
            String eventId,
            ObservationKeyEventType type,
            long occurredAtMs,
            long generation) {
        return new ObservationKeyEvent(
                eventId,
                type,
                occurredAtMs,
                null,
                null,
                null,
                "xinshou-connectivity-runner",
                null,
                null,
                generation,
                null,
                null);
    }

    private static Path connectivityDir() {
        String configured = System.getenv("DHXY_CONNECTIVITY_DIR");
        return configured == null || configured.isBlank()
                ? Path.of("target", "connectivity")
                : Path.of(configured);
    }

    private static final class ArtifactObservationClient implements ObservationClient {
        private static final ObservationInterest COMBAT_INTEREST =
                new ObservationInterest("combat-signal", 100L, null);

        private final Path output;
        private final CountDownLatch initialRequest = new CountDownLatch(1);
        private final CountDownLatch enterRequest = new CountDownLatch(1);
        private final CountDownLatch exitRequest = new CountDownLatch(1);

        private ArtifactObservationClient(Path output) {
            this.output = output;
        }

        @Override
        public ObservationResponse send(ObservationRequest request)
                throws ObservationTransportException {
            ObservationProtocolValidator.requireValid(request);
            try {
                for (ObservationKeyEvent event : request.events()) {
                    if (event.eventType() == ObservationKeyEventType.IN_COMBAT) {
                        MAPPER.writeValue(
                                output.resolve("xinshou-in-combat-observation.json").toFile(),
                                request);
                        enterRequest.countDown();
                    } else if (event.eventType() == ObservationKeyEventType.COMBAT_EXITED) {
                        MAPPER.writeValue(
                                output.resolve("xinshou-combat-exited-observation.json").toFile(),
                                request);
                        exitRequest.countDown();
                    }
                }
                initialRequest.countDown();
            } catch (Exception failure) {
                throw new ObservationTransportException(
                        ObservationTransportException.Kind.SERIALIZATION,
                        "cannot write connectivity observation artifact",
                        failure);
            }
            return ObservationProtocolValidator.requireValid(
                    new ObservationResponse(
                            ObservationProtocolValidator.CONTRACT_VERSION,
                            request.observerSeq(),
                            1L,
                            request.events().stream().map(ObservationKeyEvent::eventId).toList(),
                            List.of(COMBAT_INTEREST),
                            List.of()),
                    request);
        }
    }
}
