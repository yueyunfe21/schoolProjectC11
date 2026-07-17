package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnMatchSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnRegion;
import com.bot.dhxy.cloud.turn.protocol.TurnRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnStep;
import com.bot.dhxy.cloud.turn.protocol.TurnStepType;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowRect;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.window.execution.WindowTaskRunner;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnMatchStepExecutorContractTest {

    private static final String TEMPLATE_KEY = "images/template/match/frame.png";
    private static final TurnRegion CAPTURE_REGION = new TurnRegion(139, 242, 6, 5);

    @TempDir
    Path temporaryDirectory;

    @Test
    void matchOnlyReturnsAbsoluteCoordinatesWithoutClickingOrReturningPixels() throws Exception {
        Harness harness = harness();

        TurnMatchStepExecutor.Execution result = harness.executor.execute(
                executionWindow(),
                step(TurnMatchSpec.OnMatch.NONE, TurnMatchSpec.ResultMode.RETURN_MATCH_RESULT));

        assertTrue(result.match().found());
        assertTrue(result.match().score() >= 0.999D);
        assertEquals(142, result.match().centerX());
        assertEquals(244, result.match().centerY());
        assertEquals(new TurnRegion(141, 243, 2, 2), result.match().rectangle());
        assertFalse(result.clickRequested(), "match-only must never perform or request input");
        assertNull(result.frame(), "result-only mode must not return captured pixels");
        assertEquals(1, harness.capture.captureCalls);
        assertEquals(0, harness.client.downloadCalls);
    }

    @Test
    void clickModeReturnsOnlyAComposedClickIntentAndDoesNotOwnInput() throws Exception {
        Harness harness = harness();

        TurnMatchStepExecutor.Execution result = harness.executor.execute(
                executionWindow(),
                step(TurnMatchSpec.OnMatch.CLICK, TurnMatchSpec.ResultMode.RETURN_MATCH_RESULT));

        assertTrue(result.match().found());
        assertTrue(result.clickRequested(), "CLICK is only an intent consumed by the later action composer");
        assertNull(result.frame());
        assertEquals(1, harness.capture.captureCalls);
        assertEquals(0, harness.client.downloadCalls);
    }

    @Test
    void missReturnsNoCoordinatesAndCannotRequestAClick() throws Exception {
        BufferedImage source = new BufferedImage(6, 5, BufferedImage.TYPE_INT_ARGB);
        Harness harness = harness(source);

        TurnMatchStepExecutor.Execution result = harness.executor.execute(
                executionWindow(),
                step(TurnMatchSpec.OnMatch.CLICK, TurnMatchSpec.ResultMode.RETURN_MATCH_RESULT));

        assertFalse(result.match().found());
        assertEquals(0.0D, result.match().score());
        assertNull(result.match().centerX());
        assertNull(result.match().centerY());
        assertNull(result.match().rectangle());
        assertFalse(result.clickRequested());
        assertNull(result.frame());
    }

    @Test
    void imageResultReturnsTheSameRawMatchEvidenceFrame() throws Exception {
        Harness harness = harness();

        TurnMatchStepExecutor.Execution result = harness.executor.execute(
                executionWindow(),
                step(
                        TurnMatchSpec.OnMatch.NONE,
                        TurnMatchSpec.ResultMode.RETURN_MATCH_RESULT_AND_IMAGE));

        assertNotNull(result.frame());
        assertEquals(TurnFramePurpose.MATCH_EVIDENCE, result.frame().metadata().purpose());
        assertEquals(CAPTURE_REGION, result.frame().metadata().region());
        assertEquals(6, result.frame().metadata().width());
        assertEquals(5, result.frame().metadata().height());
        assertEquals(3, result.frame().metadata().sourceStepIndex());
        BufferedImage decoded = ImageIO.read(new java.io.ByteArrayInputStream(result.frame().pngBytes()));
        try {
            assertEquals(6, decoded.getWidth());
            assertEquals(5, decoded.getHeight());
        } finally {
            decoded.flush();
        }
    }

    @Test
    void invalidTemplateHashIsRejectedBeforeCaptureOrCloudDownload() throws Exception {
        Harness harness = harness();
        TurnStep invalid = new TurnStep(
                3,
                TurnStepType.MATCH_TEMPLATE,
                null,
                null,
                null,
                null,
                new TurnMatchSpec(
                        CAPTURE_REGION,
                        TEMPLATE_KEY,
                        "not-a-sha256",
                        0.999D,
                        TurnMatchSpec.OnMatch.NONE,
                        TurnMatchSpec.ResultMode.RETURN_MATCH_RESULT),
                null);

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> harness.executor.execute(executionWindow(), invalid));

        assertEquals("match.contentHash must be a SHA-256 hexadecimal value", failure.getMessage());
        assertEquals(0, harness.capture.captureCalls);
        assertEquals(0, harness.client.downloadCalls);
    }

    private Harness harness() throws Exception {
        return harness(sourceWithFixture());
    }

    private Harness harness(BufferedImage source) throws Exception {
        Path root = Files.createDirectory(temporaryDirectory.resolve("templates"));
        Path template = root.resolve("match/frame.png");
        Files.createDirectories(template.getParent());
        Files.write(template, fixturePng());
        NoDownloadTurnClient client = new NoDownloadTurnClient();
        SourceCaptureService capture = new SourceCaptureService(source);
        TurnCaptureStepExecutor captureExecutor = new TurnCaptureStepExecutor(capture, new TurnPngCodec());
        TurnMatchStepExecutor executor = new TurnMatchStepExecutor(
                new TurnTemplateCache(root, client), captureExecutor);
        return new Harness(executor, capture, client);
    }

    private static TurnStep step(TurnMatchSpec.OnMatch onMatch,
                                 TurnMatchSpec.ResultMode resultMode) throws Exception {
        return new TurnStep(
                3,
                TurnStepType.MATCH_TEMPLATE,
                null,
                null,
                null,
                null,
                new TurnMatchSpec(
                        CAPTURE_REGION,
                        TEMPLATE_KEY,
                        sha256(fixturePng()),
                        0.999D,
                        onMatch,
                        resultMode),
                null);
    }

    private static BufferedImage sourceWithFixture() throws IOException {
        BufferedImage source = new BufferedImage(6, 5, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                source.setRGB(x, y, 0xff000000 | ((x * 29 + y * 7) << 16)
                        | ((x * 11 + y * 31) << 8) | (x * 17 + y * 13));
            }
        }
        BufferedImage fixture = ImageIO.read(new java.io.ByteArrayInputStream(fixturePng()));
        Graphics2D graphics = source.createGraphics();
        try {
            graphics.drawImage(fixture, 2, 1, null);
        } finally {
            graphics.dispose();
            fixture.flush();
        }
        return source;
    }

    private static byte[] fixturePng() throws IOException {
        try (var input = TurnMatchStepExecutorContractTest.class
                .getResourceAsStream("/cloud-turn/v1/frame-2x2.png")) {
            if (input == null) {
                throw new IOException("missing frame-2x2.png fixture");
            }
            return input.readAllBytes();
        }
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static TurnExecutionWindow executionWindow() {
        try {
            WindowRuntimeContext context = new WindowRuntimeContext("window-7", new GameContext());
            WindowNativeBinding binding = new WindowNativeBinding(
                    "12345", "game-window-7", "GameWindow", 88L, 137, 241, 10, 8);
            TurnWindowMetadata metadata = new TurnWindowMetadata(
                    "device-1",
                    "window-7",
                    "game-window-7",
                    "12345",
                    88L,
                    new TurnWindowRect(137, 241, 10, 8),
                    false);
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

    private record Harness(TurnMatchStepExecutor executor,
                           SourceCaptureService capture,
                           NoDownloadTurnClient client) {
    }

    private static final class SourceCaptureService extends BoundWindowCaptureService {
        private final BufferedImage source;
        private int captureCalls;

        private SourceCaptureService(BufferedImage source) {
            this.source = source;
        }

        @Override
        public Optional<CaptureResult> captureRegion(WindowNativeBinding binding,
                                                     int windowBaseX,
                                                     int windowBaseY,
                                                     int x1,
                                                     int y1,
                                                     int x2,
                                                     int y2) {
            captureCalls++;
            BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = copy.createGraphics();
            try {
                graphics.drawImage(source, 0, 0, null);
            } finally {
                graphics.dispose();
            }
            return Optional.of(new CaptureResult(copy, CaptureProvider.HWND_PRINTWINDOW));
        }
    }

    private static final class NoDownloadTurnClient implements TurnClient {
        private int downloadCalls;

        @Override
        public TurnExchangeResult exchange(TurnRequest request, byte[] optionalPng) {
            throw new AssertionError("turn exchange is outside the match executor contract");
        }

        @Override
        public TurnTemplateDownload downloadTemplate(String templateKey, String ifNoneMatch) {
            downloadCalls++;
            throw new AssertionError("exact local template hash should not download");
        }
    }
}
