package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.protocol.observation.ObservationProtocolValidator;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationAnalysisResult;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationRequest;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationResponse;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowObservationTerminalFrameContractTest {

    private static final String TENANT = "tenant-1";
    private static final String DEVICE = "device-1";
    private static final String WINDOW = "window-7";
    private static final String HWND = "12345";
    private static final String TASK_RUN = "run-1";

    @Test
    void coordinateStripIsCroppedFromTheExactTerminalFrameAndAckPreventsReupload() throws Exception {
        Fixture fixture = fixture("intent-same-frame");
        armStableCandidate(fixture);

        WindowObservationSampler.SampleBatch batch = fixture.sampler.collect(List.of());

        assertEquals(1, batch.terminalFrames().size());
        assertEquals(1, batch.rois().size());
        var terminal = batch.terminalFrames().getFirst();
        var coordinate = batch.rois().getFirst();
        BufferedImage full = ImageIO.read(new ByteArrayInputStream(terminal.pngBytes()));
        BufferedImage crop = ImageIO.read(new ByteArrayInputStream(coordinate.pngBytes()));
        try {
            assertEquals(ObservationProtocolValidator.TERMINAL_FRAME_WIDTH, full.getWidth());
            assertEquals(ObservationProtocolValidator.TERMINAL_FRAME_HEIGHT, full.getHeight());
            assertEquals(coordinate.width(), crop.getWidth());
            assertEquals(coordinate.height(), crop.getHeight());
            for (int y : List.of(0, crop.getHeight() / 2, crop.getHeight() - 1)) {
                for (int x : List.of(0, crop.getWidth() / 2, crop.getWidth() - 1)) {
                    assertEquals(
                            full.getRGB(coordinate.left() + x, coordinate.top() + y),
                            crop.getRGB(x, y),
                            "coordinate pixels must come from the exact uploaded full frame");
                }
            }
        } finally {
            full.flush();
            crop.flush();
        }

        fixture.sampler.acknowledgeTerminalFrames(List.of(terminal.frameId()));
        WindowObservationSampler.SampleBatch afterAck = fixture.sampler.collect(List.of());

        assertTrue(afterAck.terminalFrames().isEmpty(), "successful delivery must fence a second full upload");
        assertEquals(1, afterAck.rois().size(), "the existing coordinate verdict may still complete");
        assertEquals(1, fixture.tracker.fullCaptureCount.get(), "ACK must not trigger another capture");
    }

    @Test
    void movementAndReplacementInvalidateAnUnacknowledgedFrame() throws Exception {
        Fixture fixture = fixture("intent-before-movement");
        armStableCandidate(fixture);
        var stale = fixture.sampler.collect(List.of()).terminalFrames().getFirst();

        fixture.tracker.coordinateColor = 0x00ffffff;
        setField(fixture.sampler, "localPathingLastSampleAtMs", 0L);
        WindowObservationSampler.SampleBatch movement = fixture.sampler.collect(List.of());
        assertTrue(movement.terminalFrames().isEmpty(), "movement must invalidate speculative full-frame work");

        fixture.context.markPathingStarted(pathingIntent("intent-replacement"));
        setField(fixture.sampler, "localPathingLastSampleAtMs", 0L);
        WindowObservationSampler.SampleBatch replacement = fixture.sampler.collect(List.of());
        assertTrue(replacement.terminalFrames().stream().noneMatch(frame -> frame.frameId() == stale.frameId()));
        assertNotEquals("intent-before-movement", replacement.pathingFacts().getFirst().intentId());

        fixture.sampler.reset();
        assertTrue(fixture.sampler.collect(List.of()).terminalFrames().isEmpty(),
                "runner stop/reset must not retain an old terminal frame");
    }

    @Test
    void arrivedFactUnlocksOnlyTheExactFrameAndGenerationUsedForItsCoordinateVerdict() throws Exception {
        Fixture fixture = fixture("intent-exact-arrived-gate");
        armStableCandidate(fixture);
        var terminal = fixture.sampler.collect(List.of()).terminalFrames().getFirst();

        fixture.sampler.acceptAnalysisResults(List.of(new ObservationAnalysisResult(
                "analysis-arrived",
                "PATHING_COORDINATE_RESOLVED",
                "coordinate-strip",
                terminal.intentId(),
                null,
                null,
                null,
                "灵兽村",
                112,
                93,
                null)));

        var arrived = fixture.sampler.collect(List.of()).pathingFacts().getFirst();
        assertEquals("ARRIVED", arrived.state().name());
        assertEquals(terminal.frameId(), arrived.terminalFrameId());
        assertEquals(terminal.pathingGeneration(), arrived.terminalFrameGeneration());
    }

    @Test
    void runnerUploadsOneFrameAndAcknowledgesItAfterOneSuccessfulExchange() throws Exception {
        Fixture fixture = fixture("intent-runner-once");
        armStableCandidate(fixture);
        assertEquals(1, fixture.sampler.collect(List.of()).terminalFrames().size());
        RecordingClient client = new RecordingClient(2);
        WindowObservationRunner runner = new WindowObservationRunner(
                client, TENANT, DEVICE, WINDOW, HWND, "XIULUO_V2", TASK_RUN,
                fixture.sampler, 50L);
        try {
            runner.start();
            assertTrue(client.handled.await(3, TimeUnit.SECONDS));
        } finally {
            runner.requestStop();
            assertTrue(runner.awaitStopped(Duration.ofSeconds(3)));
        }

        assertEquals(1, client.requests.get(0).terminalFrames().size());
        assertTrue(client.requests.get(1).terminalFrames().isEmpty());
        var uploaded = client.requests.get(0).terminalFrames().getFirst();
        assertEquals(TENANT, uploaded.tenantId());
        assertEquals(DEVICE, uploaded.deviceId());
        assertEquals(WINDOW, uploaded.windowId());
        assertEquals(HWND, uploaded.hwnd());
        assertEquals(TASK_RUN, uploaded.taskRunId());
        assertEquals("intent-runner-once", uploaded.intentId());
        assertEquals(1, fixture.tracker.fullCaptureCount.get());
    }

    @Test
    void transportFailureResendsTheSameFrameUntilOneSuccessfulAcknowledgement() throws Exception {
        Fixture fixture = fixture("intent-transport-retry");
        armStableCandidate(fixture);
        assertEquals(1, fixture.sampler.collect(List.of()).terminalFrames().size());
        FailOnceClient client = new FailOnceClient(3);
        WindowObservationRunner runner = new WindowObservationRunner(
                client, TENANT, DEVICE, WINDOW, HWND, "XIULUO_V2", TASK_RUN,
                fixture.sampler, 50L);
        try {
            runner.start();
            assertTrue(client.handled.await(3, TimeUnit.SECONDS));
        } finally {
            runner.requestStop();
            assertTrue(runner.awaitStopped(Duration.ofSeconds(3)));
        }

        var first = client.requests.get(0).terminalFrames().getFirst();
        var retried = client.requests.get(1).terminalFrames().getFirst();
        assertEquals(first.frameId(), retried.frameId());
        assertEquals(first.pathingGeneration(), retried.pathingGeneration());
        assertArrayEquals(first.pngBytes(), retried.pngBytes());
        assertTrue(client.requests.get(2).terminalFrames().isEmpty(),
                "one successful response must invalidate later full-frame uploads");
        assertEquals(1, fixture.tracker.fullCaptureCount.get());
    }

    private static Fixture fixture(String intentId) {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        context.setNativeBinding(new WindowNativeBinding(HWND, "title", "class", 7L, 0, 0, 1024, 768));
        context.markPathingStarted(pathingIntent(intentId));
        WindowTaskContextHolder holder = new WindowTaskContextHolder(new WindowIsolationProperties());
        PatternTracker tracker = new PatternTracker();
        CoordinateHelper coordinateHelper = new CoordinateHelper(tracker, null);
        WindowObservationSampler sampler = new WindowObservationSampler(
                context,
                holder,
                tracker,
                coordinateHelper,
                new DialogService(tracker, coordinateHelper),
                new InputSequences(null),
                TASK_RUN,
                false);
        return new Fixture(context, sampler, tracker);
    }

    private static void armStableCandidate(Fixture fixture) throws Exception {
        fixture.sampler.collect(List.of());
        long now = System.currentTimeMillis();
        setField(fixture.sampler, "localPathingLastSampleAtMs", 0L);
        setField(fixture.sampler, "localPathingLastChangedAtMs", now - 700L);
        setField(fixture.sampler, "localPathingMovementObservedAtMs", now - 700L);
    }

    private static WindowPathingIntent pathingIntent(String intentId) {
        return WindowPathingIntent.builder()
                .intentId(intentId)
                .source("navigation:terminal-frame-test")
                .targetMapName("灵兽村")
                .targetX(112)
                .targetY(93)
                .tolerance(5)
                .type(WindowPathingIntentType.TARGETED)
                .createdAtMs(System.currentTimeMillis() - 5_000L)
                .build();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private record Fixture(
            WindowRuntimeContext context,
            WindowObservationSampler sampler,
            PatternTracker tracker) {
    }

    private static final class PatternTracker extends GameClientTracker {
        private final AtomicInteger fullCaptureCount = new AtomicInteger();
        private volatile int coordinateColor;

        private PatternTracker() {
            super(null, null, null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public boolean refreshWindowState() {
            return true;
        }

        @Override
        public int getWindowBaseX() {
            return 0;
        }

        @Override
        public int getWindowBaseY() {
            return 0;
        }

        @Override
        public BufferedImage captureToMemory(String elementName, int x1, int y1, int x2, int y2) {
            int width = x2 - x1;
            int height = y2 - y1;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            if (width == 1024 && height == 768) {
                fullCaptureCount.incrementAndGet();
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        image.setRGB(x, y, ((x & 0xff) << 16) | ((y & 0xff) << 8) | ((x + y) & 0xff));
                    }
                }
            } else {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        image.setRGB(x, y, coordinateColor);
                    }
                }
            }
            return image;
        }
    }

    private static final class RecordingClient implements ObservationClient {
        private final List<ObservationRequest> requests = new CopyOnWriteArrayList<>();
        private final CountDownLatch handled;

        private RecordingClient(int expectedRequests) {
            this.handled = new CountDownLatch(expectedRequests);
        }

        @Override
        public ObservationResponse send(ObservationRequest request) {
            requests.add(request);
            handled.countDown();
            return ObservationProtocolValidator.requireValid(
                    new ObservationResponse(
                            ObservationProtocolValidator.CONTRACT_VERSION,
                            request.observerSeq(),
                            0L,
                            List.of(),
                            List.of(),
                            List.of()),
                    request);
        }
    }

    private static final class FailOnceClient implements ObservationClient {
        private final List<ObservationRequest> requests = new CopyOnWriteArrayList<>();
        private final CountDownLatch handled;
        private final AtomicInteger attempts = new AtomicInteger();

        private FailOnceClient(int expectedRequests) {
            this.handled = new CountDownLatch(expectedRequests);
        }

        @Override
        public ObservationResponse send(ObservationRequest request) throws ObservationTransportException {
            requests.add(request);
            handled.countDown();
            if (attempts.getAndIncrement() == 0) {
                throw new ObservationTransportException(
                        ObservationTransportException.Kind.NETWORK,
                        "synthetic uncertain delivery");
            }
            return ObservationProtocolValidator.requireValid(
                    new ObservationResponse(
                            ObservationProtocolValidator.CONTRACT_VERSION,
                            request.observerSeq(),
                            0L,
                            List.of(),
                            List.of(),
                            List.of()),
                    request);
        }
    }
}
