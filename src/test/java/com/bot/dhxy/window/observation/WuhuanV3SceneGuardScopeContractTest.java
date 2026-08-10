package com.bot.dhxy.window.observation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WuhuanV3SceneGuardScopeContractTest {

    private static final Path FACTORY = Path.of(
            "src/main/java/com/bot/dhxy/window/observation/SpringObservationRunnerFactory.java");

    @Test
    void terminalFrameSceneGuardIsBoundOnlyForWuhuanV3() throws IOException {
        String source = Files.readString(FACTORY, StandardCharsets.UTF_8);
        int bind = source.indexOf("sampler.bindUiCleanerService(uiCleanerService)");
        int condition = source.lastIndexOf("if (", bind);
        String guardedBinding = source.substring(condition, bind);

        assertTrue(bind > 0 && condition > 0);
        assertTrue(guardedBinding.contains("\"WUHUAN_V3\".equalsIgnoreCase(taskCode)"),
                "the arrival scene guard belongs only to 五环 V3");
        assertFalse(guardedBinding.contains("sampler != null)"),
                "a sampler alone must never authorize cross-task UI cleanup");
    }
}
