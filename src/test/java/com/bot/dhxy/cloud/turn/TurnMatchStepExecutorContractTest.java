package com.bot.dhxy.cloud.turn;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnMatchStepExecutorContractTest {
    @Test
    void matchReturnsCloudIntentAndEvidenceWithoutOwningPhysicalInput() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/cloud/turn/TurnMatchStepExecutor.java"));
        assertTrue(source.contains("TurnMatchResult"));
        assertTrue(source.contains("TurnFramePurpose.MATCH_EVIDENCE"));
        assertTrue(source.contains("templateCache"));
        assertFalse(source.contains("InputProvider"));
        assertFalse(source.contains("InputActionQueue"));
    }
}
