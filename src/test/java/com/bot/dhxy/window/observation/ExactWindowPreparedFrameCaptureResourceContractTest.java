package com.bot.dhxy.window.observation;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExactWindowPreparedFrameCaptureResourceContractTest {

    @Test
    void capturedImageGeometryIsSavedBeforeFinallyFlush() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/observation/ExactWindowPreparedFrameCapture.java"));
        int width = source.indexOf("int width = image.getWidth();");
        int height = source.indexOf("int height = image.getHeight();");
        int tryBlock = source.indexOf("try {", height);
        int response = source.indexOf("new ObservationPreparedFrame(", tryBlock);
        int finallyBlock = source.indexOf("finally {", response);
        int flush = source.indexOf("image.flush();", finallyBlock);

        assertTrue(width >= 0 && height > width,
                "image dimensions must be captured before resource release");
        assertTrue(tryBlock > height && response > tryBlock && finallyBlock > response && flush > finallyBlock,
                "PNG validation and response construction must be protected by finally image.flush()");
    }
}
