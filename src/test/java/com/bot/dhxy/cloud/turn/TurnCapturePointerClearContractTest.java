package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnCaptureSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnRegion;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowRect;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionType;
import com.bot.dhxy.window.execution.WindowTaskRunner;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnCapturePointerClearContractTest {

    private static final TurnWindowRect DEFAULT_WINDOW = new TurnWindowRect(100, 200, 800, 600);
    private static final TurnRegion DEFAULT_REGION = new TurnRegion(200, 300, 100, 80);

    @Test
    void absentPolicyPerformsNoPointerReadOrInputAndCapturesExactlyOnce() {
        Harness harness = new Harness(DEFAULT_WINDOW, new Point(220, 320), false);

        TurnFrame frame = completedFrame(harness.executor.execute(
                harness.window,
                new TurnCaptureSpec(DEFAULT_REGION, TurnCaptureSpec.ResultMode.UPLOAD_IMAGE),
                2));

        assertEquals(0, harness.pointerReads.get());
        assertEquals(0, harness.input.submissions);
        assertSingleExactCapture(harness, frame, DEFAULT_REGION, 2);
        assertEquals(List.of("capture"), harness.events);
    }

    @Test
    void nullPointerPerformsNoInputAndCapturesExactlyOnce() {
        Harness harness = new Harness(DEFAULT_WINDOW, null, false);

        TurnFrame frame = completedFrame(harness.executor.execute(harness.window, captureSpec(120, 250), 3));

        assertEquals(1, harness.pointerReads.get());
        assertEquals(0, harness.input.submissions);
        assertSingleExactCapture(harness, frame, DEFAULT_REGION, 3);
        assertEquals(List.of("capture"), harness.events);
    }

    @Test
    void pointerOutsidePaddedRegionPerformsNoInputAndCapturesExactlyOnce() {
        Harness harness = new Harness(DEFAULT_WINDOW, new Point(187, 320), false);

        TurnFrame frame = completedFrame(harness.executor.execute(harness.window, captureSpec(120, 250), 4));

        assertEquals(1, harness.pointerReads.get());
        assertEquals(0, harness.input.submissions);
        assertSingleExactCapture(harness, frame, DEFAULT_REGION, 4);
        assertEquals(List.of("capture"), harness.events);
    }

    @Test
    void pointerOnInclusivePaddedBoundaryMovesOnceThenCaptures() {
        Harness harness = new Harness(DEFAULT_WINDOW, new Point(188, 320), false);

        TurnFrame frame = completedFrame(harness.executor.execute(harness.window, captureSpec(120, 250), 5));

        assertEquals(1, harness.pointerReads.get());
        assertOrderedMoveWait(harness, 120, 250, 300);
        assertSingleExactCapture(harness, frame, DEFAULT_REGION, 5);
        assertEquals(List.of("input", "capture"), harness.events);
    }

    @Test
    void pointerInsideSubmitsOneExactMoveWaitQueueThenCapturesTheSameHwnd() {
        Harness harness = new Harness(DEFAULT_WINDOW, new Point(220, 320), false);

        TurnFrame frame = completedFrame(harness.executor.execute(harness.window, captureSpec(120, 250), 6));

        assertEquals(1, harness.pointerReads.get());
        assertOrderedMoveWait(harness, 120, 250, 300);
        assertSame(harness.window.context(), harness.input.observedContext);
        assertSingleExactCapture(harness, frame, DEFAULT_REGION, 6);
        assertSame(harness.window.binding(), harness.capture.lastBinding);
        assertEquals(List.of("input", "capture"), harness.events);
    }

    @Test
    void queueFalseEndsWithoutCaptureOrRetry() {
        Harness harness = new Harness(DEFAULT_WINDOW, new Point(220, 320), false);
        harness.input.result = false;

        assertThrows(IllegalStateException.class,
                () -> harness.executor.execute(harness.window, captureSpec(120, 250), 7));

        assertEquals(1, harness.pointerReads.get());
        assertEquals(1, harness.input.submissions);
        assertEquals(0, harness.capture.totalCalls());
        assertEquals(List.of("input"), harness.events);
    }

    @Test
    void stoppedWindowEndsBeforePointerInputOrCapture() {
        Harness harness = new Harness(DEFAULT_WINDOW, new Point(220, 320), true);

        assertThrows(IllegalStateException.class,
                () -> harness.executor.execute(harness.window, captureSpec(120, 250), 8));

        assertEquals(0, harness.pointerReads.get());
        assertEquals(0, harness.input.submissions);
        assertEquals(0, harness.capture.totalCalls());
        assertEquals(List.of(), harness.events);
    }

    @Test
    void preInterruptedThreadEndsBeforePointerInputOrCaptureAndPreservesInterrupt() {
        Harness harness = new Harness(DEFAULT_WINDOW, new Point(220, 320), false);
        Thread.currentThread().interrupt();
        try {
            assertThrows(IllegalStateException.class,
                    () -> harness.executor.execute(harness.window, captureSpec(120, 250), 9));

            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(0, harness.pointerReads.get());
            assertEquals(0, harness.input.submissions);
            assertEquals(0, harness.capture.totalCalls());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void queueInterruptionEndsWithoutCaptureOrRetryAndPreservesInterrupt() {
        Harness harness = new Harness(DEFAULT_WINDOW, new Point(220, 320), false);
        harness.input.result = false;
        harness.input.interruptBeforeReturn = true;
        try {
            assertThrows(IllegalStateException.class,
                    () -> harness.executor.execute(harness.window, captureSpec(120, 250), 10));

            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(1, harness.pointerReads.get());
            assertEquals(1, harness.input.submissions);
            assertEquals(0, harness.capture.totalCalls());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void queueExceptionEndsWithoutCaptureOrRetry() {
        Harness harness = new Harness(DEFAULT_WINDOW, new Point(220, 320), false);
        harness.input.failure = new IllegalStateException("queue boom");

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> harness.executor.execute(harness.window, captureSpec(120, 250), 11));

        assertEquals("queue boom", failure.getMessage());
        assertEquals(1, harness.pointerReads.get());
        assertEquals(1, harness.input.submissions);
        assertEquals(0, harness.capture.totalCalls());
    }

    @Test
    void targetOutsideRefreshedWindowFailsBeforePointerInputOrCapture() {
        Harness harness = new Harness(DEFAULT_WINDOW, new Point(220, 320), false);

        assertThrows(IllegalArgumentException.class,
                () -> harness.executor.execute(harness.window, captureSpec(99, 250), 12));

        assertEquals(0, harness.pointerReads.get());
        assertEquals(0, harness.input.submissions);
        assertEquals(0, harness.capture.totalCalls());
    }

    @Test
    void targetOnPaddedRegionBoundaryFailsBeforePointerInputOrCapture() {
        Harness harness = new Harness(DEFAULT_WINDOW, new Point(220, 320), false);

        assertThrows(IllegalArgumentException.class,
                () -> harness.executor.execute(harness.window, captureSpec(188, 320), 13));

        assertEquals(0, harness.pointerReads.get());
        assertEquals(0, harness.input.submissions);
        assertEquals(0, harness.capture.totalCalls());
    }

    @Test
    void negativeMonitorOriginKeepsExactSignedCoordinatesAndOneCapture() {
        TurnWindowRect negativeWindow = new TurnWindowRect(-1_600, -300, 1_000, 800);
        TurnRegion negativeRegion = new TurnRegion(-1_500, -200, 100, 80);
        Harness harness = new Harness(negativeWindow, new Point(-1_450, -180), false);
        TurnCaptureSpec spec = new TurnCaptureSpec(
                negativeRegion,
                TurnCaptureSpec.ResultMode.UPLOAD_IMAGE,
                new TurnCaptureSpec.ClearPointerIfOverRegion(12, -700, 400, 300));

        TurnFrame frame = completedFrame(harness.executor.execute(harness.window, spec, 14));

        assertEquals(1, harness.pointerReads.get());
        assertOrderedMoveWait(harness, -700, 400, 300);
        assertSingleExactCapture(harness, frame, negativeRegion, 14);
        assertEquals(-1_600, harness.capture.windowBaseX);
        assertEquals(-300, harness.capture.windowBaseY);
        assertEquals(-1_500, harness.capture.x1);
        assertEquals(-200, harness.capture.y1);
        assertEquals(-1_400, harness.capture.x2);
        assertEquals(-120, harness.capture.y2);
        assertEquals(List.of("input", "capture"), harness.events);
    }

    private static TurnCaptureSpec captureSpec(int targetX, int targetY) {
        return new TurnCaptureSpec(
                DEFAULT_REGION,
                TurnCaptureSpec.ResultMode.UPLOAD_IMAGE,
                new TurnCaptureSpec.ClearPointerIfOverRegion(12, targetX, targetY, 300));
    }

    private static TurnFrame completedFrame(TurnCaptureStepExecutor.Execution execution) {
        assertEquals(TurnCaptureStepExecutor.Status.COMPLETED, execution.status());
        assertEquals(TurnCaptureStepExecutor.Code.OK, execution.code());
        return execution.frame();
    }

    private static void assertOrderedMoveWait(Harness harness, int targetX, int targetY, int settleMs) {
        assertEquals(1, harness.input.submissions);
        assertEquals(List.of(InputActionType.MOVE_MOUSE, InputActionType.SLEEP),
                harness.input.lastActions.stream().map(InputAction::getType).toList());
        InputAction move = harness.input.lastActions.get(0);
        InputAction wait = harness.input.lastActions.get(1);
        assertEquals(targetX, move.getX());
        assertEquals(targetY, move.getY());
        assertEquals(0, move.getDelayMs());
        assertEquals(settleMs, wait.getDelayMs());
    }

    private static void assertSingleExactCapture(
            Harness harness,
            TurnFrame frame,
            TurnRegion expectedRegion,
            int sourceStepIndex) {
        assertEquals(0, harness.capture.fullWindowCalls);
        assertEquals(1, harness.capture.regionCalls);
        assertSame(harness.window.binding(), harness.capture.lastBinding);
        assertEquals(expectedRegion, frame.metadata().region());
        assertEquals(expectedRegion.width(), frame.metadata().width());
        assertEquals(expectedRegion.height(), frame.metadata().height());
        assertEquals(sourceStepIndex, frame.metadata().sourceStepIndex());
    }

    private static TurnExecutionWindow executionWindow(TurnWindowRect rect, boolean stopRequested) {
        try {
            WindowRuntimeContext context = new WindowRuntimeContext("window-pointer-clear", new GameContext());
            WindowNativeBinding binding = new WindowNativeBinding(
                    "12345",
                    "game-window-pointer-clear",
                    "GameWindow",
                    88L,
                    rect.left(),
                    rect.top(),
                    rect.width(),
                    rect.height());
            TurnWindowMetadata metadata = new TurnWindowMetadata(
                    "device-1",
                    "window-pointer-clear",
                    "game-window-pointer-clear",
                    "12345",
                    88L,
                    rect,
                    false,
                    stopRequested);
            Constructor<TurnExecutionWindow> constructor = TurnExecutionWindow.class.getDeclaredConstructor(
                    WindowTaskRunner.class,
                    WindowRuntimeContext.class,
                    WindowNativeBinding.class,
                    TurnWindowMetadata.class);
            constructor.setAccessible(true);
            return constructor.newInstance(null, context, binding, metadata);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("cannot construct isolated turn execution window", e);
        }
    }

    private static final class Harness {
        private final List<String> events = new ArrayList<>();
        private final AtomicInteger pointerReads = new AtomicInteger();
        private final WindowTaskContextHolder contextHolder =
                new WindowTaskContextHolder(new WindowIsolationProperties());
        private final RecordingCaptureService capture = new RecordingCaptureService(events);
        private final RecordingInputSequences input = new RecordingInputSequences(contextHolder, events);
        private final TurnExecutionWindow window;
        private final TurnCaptureStepExecutor executor;

        private Harness(TurnWindowRect rect, Point pointer, boolean stopRequested) {
            this.window = executionWindow(rect, stopRequested);
            this.executor = new TurnCaptureStepExecutor(
                    capture,
                    new TurnPngCodec(),
                    input,
                    contextHolder,
                    () -> {
                        pointerReads.incrementAndGet();
                        return pointer;
                    });
        }
    }

    private static final class RecordingInputSequences extends InputSequences {
        private final WindowTaskContextHolder contextHolder;
        private final List<String> events;
        private boolean result = true;
        private boolean interruptBeforeReturn;
        private RuntimeException failure;
        private int submissions;
        private List<InputAction> lastActions = List.of();
        private WindowRuntimeContext observedContext;

        private RecordingInputSequences(WindowTaskContextHolder contextHolder, List<String> events) {
            super(null);
            this.contextHolder = contextHolder;
            this.events = events;
        }

        @Override
        public boolean submitAndWait(String description, List<InputAction> actions) {
            submissions++;
            events.add("input");
            observedContext = contextHolder.rawCurrent().orElse(null);
            lastActions = List.copyOf(actions);
            if (failure != null) {
                throw failure;
            }
            if (interruptBeforeReturn) {
                Thread.currentThread().interrupt();
            }
            return result;
        }
    }

    private static final class RecordingCaptureService extends BoundWindowCaptureService {
        private final List<String> events;
        private int fullWindowCalls;
        private int regionCalls;
        private WindowNativeBinding lastBinding;
        private int windowBaseX;
        private int windowBaseY;
        private int x1;
        private int y1;
        private int x2;
        private int y2;

        private RecordingCaptureService(List<String> events) {
            this.events = events;
        }

        @Override
        public Optional<CaptureResult> captureWindow(WindowNativeBinding binding) {
            fullWindowCalls++;
            events.add("capture");
            lastBinding = binding;
            return Optional.of(result(binding.getWidth(), binding.getHeight()));
        }

        @Override
        public Optional<CaptureResult> captureRegion(
                WindowNativeBinding binding,
                int windowBaseX,
                int windowBaseY,
                int x1,
                int y1,
                int x2,
                int y2) {
            regionCalls++;
            events.add("capture");
            lastBinding = binding;
            this.windowBaseX = windowBaseX;
            this.windowBaseY = windowBaseY;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            return Optional.of(result(x2 - x1, y2 - y1));
        }

        private int totalCalls() {
            return fullWindowCalls + regionCalls;
        }

        private CaptureResult result(int width, int height) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, 0xff336699);
            return new CaptureResult(image, CaptureProvider.HWND_PRINTWINDOW);
        }
    }
}
