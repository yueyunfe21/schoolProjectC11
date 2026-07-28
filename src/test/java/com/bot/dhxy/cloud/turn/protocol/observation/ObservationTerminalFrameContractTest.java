package com.bot.dhxy.cloud.turn.protocol.observation;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObservationTerminalFrameContractTest {

    @Test
    void acceptsOneExactTerminalFrame() {
        ObservationRequest request = request(List.of(frame("window-7", "intent-3", png())));

        assertSame(request, ObservationProtocolValidator.requireValid(request));
    }

    @Test
    void rejectsASecondTerminalFrameInOneRequest() {
        ObservationTerminalFrame frame = frame("window-7", "intent-3", png());

        assertThrows(IllegalArgumentException.class,
                () -> ObservationProtocolValidator.requireValid(request(List.of(frame, frame))));
    }

    @Test
    void rejectsIdentityMismatch() {
        assertThrows(IllegalArgumentException.class,
                () -> ObservationProtocolValidator.requireValid(
                        request(List.of(frame("window-stale", "intent-3", png())))));
    }

    @Test
    void rejectsOversizedTerminalFramePayload() {
        byte[] oversized = new byte[ObservationProtocolValidator.MAX_TERMINAL_FRAME_PNG_BYTES + 1];
        System.arraycopy(png(), 0, oversized, 0, png().length);

        assertThrows(IllegalArgumentException.class,
                () -> ObservationProtocolValidator.requireValid(
                        request(List.of(frame("window-7", "intent-3", oversized)))));
    }

    @Test
    void rejectsPngWhoseIhdrGeometryDoesNotMatchTheExactWindow() {
        assertThrows(IllegalArgumentException.class,
                () -> ObservationProtocolValidator.requireValid(
                        request(List.of(frame("window-7", "intent-3", png(812, 663))))));
    }

    @Test
    void rejectsTruncatedIhdrEvenWhenThePngSignatureIsPresent() {
        byte[] truncated = Arrays.copyOf(png(), 20);

        assertThrows(IllegalArgumentException.class,
                () -> ObservationProtocolValidator.requireValid(
                        request(List.of(frame("window-7", "intent-3", truncated)))));
    }

    @Test
    void rejectsMalformedLeadingIhdrChunk() {
        byte[] malformed = png().clone();
        malformed[12] = 'B';

        assertThrows(IllegalArgumentException.class,
                () -> ObservationProtocolValidator.requireValid(
                        request(List.of(frame("window-7", "intent-3", malformed)))));
    }

    @Test
    void ordinaryRoiStillRejectsWholeWindowDimensions() {
        ObservationRoi wholeWindowRoi = new ObservationRoi(
                "ordinary", 0, 0, 1024, 768, png(), null, null, null, null);
        ObservationRequest request = new ObservationRequest(
                ObservationProtocolValidator.CONTRACT_VERSION,
                "tenant-1", "device-1", "window-7", "12345", "XIULUO_V2", "run-1",
                9L, 2_000L, 0L, null, null, null, "test", null,
                List.of(), List.of(), List.of(), List.of(wholeWindowRoi),
                List.of(), List.of(), List.of());

        assertThrows(IllegalArgumentException.class,
                () -> ObservationProtocolValidator.requireValid(request));
    }

    private static ObservationRequest request(List<ObservationTerminalFrame> frames) {
        return new ObservationRequest(
                ObservationProtocolValidator.CONTRACT_VERSION,
                "tenant-1", "device-1", "window-7", "12345", "XIULUO_V2", "run-1",
                9L, 2_000L, 0L, "intent-3", null, null, "test", null,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), frames);
    }

    private static ObservationTerminalFrame frame(String windowId, String intentId, byte[] png) {
        return new ObservationTerminalFrame(
                41L, 7L, "tenant-1", "device-1", windowId, "12345", "run-1", intentId,
                0, 0, 1024, 768, "PNG", 1_900L, png);
    }

    private static byte[] png() {
        return png(1024, 768);
    }

    private static byte[] png(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "PNG", output)) {
                throw new AssertionError("PNG writer unavailable");
            }
            return output.toByteArray();
        } catch (IOException failure) {
            throw new AssertionError("failed to create PNG fixture", failure);
        } finally {
            image.flush();
        }
    }
}
