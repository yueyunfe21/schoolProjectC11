package com.bot.dhxy.cloud.turn;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnCaptureStepExecutorContractTest {
    @Test
    void captureUsesFrozenExactBindingAndPreservesAbsoluteRegionMetadata() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java"));
        assertTrue(source.contains("window.binding()"));
        assertTrue(source.contains("window.metadata().windowRect()"));
        assertTrue(source.contains("pngCodec.encode(image, purpose, actualRegion, sourceStepIndex)"));
        assertTrue(source.contains("TurnFramePurpose"));
    }
}
