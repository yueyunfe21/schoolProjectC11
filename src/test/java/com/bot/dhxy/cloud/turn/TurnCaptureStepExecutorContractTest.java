package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnCaptureSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnRegion;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowRect;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.window.execution.WindowTaskRunner;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TurnCaptureStepExecutorContractTest {

    @Test
    void fullWindowCapturePreservesTheRealNonzeroWindowOriginAndPixelSize() throws Exception {
        RecordingCaptureService capture = new RecordingCaptureService();
        capture.fullWidth = 4;
        capture.fullHeight = 3;
        TurnCaptureStepExecutor executor = new TurnCaptureStepExecutor(capture, new TurnPngCodec());
        TurnExecutionWindow window = executionWindow(false);

        TurnCaptureStepExecutor.Execution execution = executor.execute(
                window,
                new TurnCaptureSpec(null, TurnCaptureSpec.ResultMode.UPLOAD_IMAGE),
                2);
        TurnFrame frame = completedFrame(execution);

        assertEquals(1, capture.fullWindowCalls);
        assertEquals(0, capture.regionCalls);
        assertSame(window.binding(), capture.lastBinding);
        assertEquals(TurnFramePurpose.CAPTURE, frame.metadata().purpose());
        assertEquals(new TurnRegion(137, 241, 4, 3), frame.metadata().region());
        assertEquals(4, frame.metadata().width());
        assertEquals(3, frame.metadata().height());
        assertEquals(2, frame.metadata().sourceStepIndex());
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(frame.pngBytes()));
        try {
            assertEquals(4, decoded.getWidth(), "capture must remain one source pixel per PNG pixel");
            assertEquals(3, decoded.getHeight(), "capture must not scale the bound window");
            assertEquals(samplePixel(0, 0), decoded.getRGB(0, 0));
            assertEquals(samplePixel(2, 1), decoded.getRGB(2, 1));
            assertEquals(samplePixel(3, 2), decoded.getRGB(3, 2));
        } finally {
            decoded.flush();
        }
    }

    @Test
    void roiCapturePassesScreenAbsoluteBoundsWithTheRealWindowBase() {
        RecordingCaptureService capture = new RecordingCaptureService();
        TurnCaptureStepExecutor executor = new TurnCaptureStepExecutor(capture, new TurnPngCodec());
        TurnRegion roi = new TurnRegion(139, 242, 2, 2);

        TurnFrame frame = executor.capture(
                executionWindow(false), roi, TurnFramePurpose.MATCH_EVIDENCE, 4);

        assertEquals(0, capture.fullWindowCalls);
        assertEquals(1, capture.regionCalls);
        assertEquals(137, capture.windowBaseX);
        assertEquals(241, capture.windowBaseY);
        assertEquals(139, capture.x1);
        assertEquals(242, capture.y1);
        assertEquals(141, capture.x2);
        assertEquals(244, capture.y2);
        assertEquals(roi, frame.metadata().region());
        assertEquals(2, frame.metadata().width());
        assertEquals(2, frame.metadata().height());
    }

    @Test
    void laterContextBindingDriftCannotReplaceTheImmutableCaptureSnapshot() {
        RecordingCaptureService capture = new RecordingCaptureService();
        TurnCaptureStepExecutor executor = new TurnCaptureStepExecutor(capture, new TurnPngCodec());
        TurnExecutionWindow window = executionWindow(false);
        window.context().setNativeBinding(window.binding());
        WindowNativeBinding driftedBinding = new WindowNativeBinding(
                "98765", "different-game-window", "GameWindow", 99L, 800, 600, 20, 20);
        window.context().setNativeBinding(driftedBinding);

        TurnCaptureStepExecutor.Execution execution = executor.execute(
                window,
                new TurnCaptureSpec(null, TurnCaptureSpec.ResultMode.UPLOAD_IMAGE),
                5);
        TurnFrame frame = completedFrame(execution);

        assertSame(window.binding(), capture.lastBinding, "capture must use the action's refreshed snapshot");
        assertEquals("12345", capture.lastBinding.getNativeHandle());
        assertSame(driftedBinding, window.context().getNativeBinding());
        assertEquals("98765", window.context().getNativeBinding().getNativeHandle());
        assertEquals(new TurnRegion(137, 241, 4, 3), frame.metadata().region());
        assertEquals(1, capture.fullWindowCalls);
        assertEquals(0, capture.regionCalls);
    }

    @Test
    void rejectsAnOutOfWindowRoiBeforeCallingTheCaptureProvider() {
        RecordingCaptureService capture = new RecordingCaptureService();
        TurnCaptureStepExecutor executor = new TurnCaptureStepExecutor(capture, new TurnPngCodec());

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> executor.capture(
                        executionWindow(false),
                        new TurnRegion(136, 241, 2, 2),
                        TurnFramePurpose.CAPTURE,
                        0));

        assertEquals("capture region is outside the refreshed window rectangle", failure.getMessage());
        assertEquals(0, capture.fullWindowCalls);
        assertEquals(0, capture.regionCalls);
    }

    @Test
    void reportsAnEmptyBackgroundCaptureAsAClosedFailure() {
        RecordingCaptureService capture = new RecordingCaptureService();
        capture.returnEmpty = true;
        TurnCaptureStepExecutor executor = new TurnCaptureStepExecutor(capture, new TurnPngCodec());

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> executor.execute(
                        executionWindow(false),
                        new TurnCaptureSpec(null, TurnCaptureSpec.ResultMode.UPLOAD_IMAGE),
                        0));

        assertEquals("Background HWND capture failed for window window-7", failure.getMessage());
        assertEquals(1, capture.fullWindowCalls);
    }

    private static TurnExecutionWindow executionWindow(boolean stopRequested) {
        try {
            WindowRuntimeContext context = new WindowRuntimeContext("window-7", new GameContext());
            WindowNativeBinding binding = new WindowNativeBinding(
                    "12345", "game-window-7", "GameWindow", 88L, 137, 241, 4, 3);
            TurnWindowMetadata metadata = new TurnWindowMetadata(
                    "device-1",
                    "window-7",
                    "game-window-7",
                    "12345",
                    88L,
                    new TurnWindowRect(137, 241, 4, 3),
                    stopRequested);
            Constructor<TurnExecutionWindow> constructor = TurnExecutionWindow.class.getDeclaredConstructor(
                    WindowTaskRunner.class,
                    WindowRuntimeContext.class,
                    WindowNativeBinding.class,
                    TurnWindowMetadata.class);
            constructor.setAccessible(true);
            return constructor.newInstance(null, context, binding, metadata);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("cannot construct an isolated turn execution window", e);
        }
    }

    private static int samplePixel(int x, int y) {
        return 0xff000000 | ((x * 53) << 16) | ((y * 71) << 8) | (x + y + 1);
    }

    private static TurnFrame completedFrame(TurnCaptureStepExecutor.Execution execution) {
        assertEquals(TurnCaptureStepExecutor.Status.COMPLETED, execution.status());
        assertEquals(TurnCaptureStepExecutor.Code.OK, execution.code());
        return execution.frame();
    }

    private static final class RecordingCaptureService extends BoundWindowCaptureService {
        private int fullWidth = 4;
        private int fullHeight = 3;
        private boolean returnEmpty;
        private int fullWindowCalls;
        private int regionCalls;
        private WindowNativeBinding lastBinding;
        private int windowBaseX;
        private int windowBaseY;
        private int x1;
        private int y1;
        private int x2;
        private int y2;

        @Override
        public Optional<CaptureResult> captureWindow(WindowNativeBinding binding) {
            fullWindowCalls++;
            lastBinding = binding;
            return returnEmpty
                    ? Optional.empty()
                    : Optional.of(result(fullWidth, fullHeight));
        }

        @Override
        public Optional<CaptureResult> captureRegion(WindowNativeBinding binding,
                                                     int windowBaseX,
                                                     int windowBaseY,
                                                     int x1,
                                                     int y1,
                                                     int x2,
                                                     int y2) {
            regionCalls++;
            lastBinding = binding;
            this.windowBaseX = windowBaseX;
            this.windowBaseY = windowBaseY;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            return returnEmpty
                    ? Optional.empty()
                    : Optional.of(result(Math.abs(x2 - x1), Math.abs(y2 - y1)));
        }

        private CaptureResult result(int width, int height) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.setRGB(x, y, samplePixel(x, y));
                }
            }
            return new CaptureResult(image, CaptureProvider.HWND_PRINTWINDOW);
        }
    }
}
