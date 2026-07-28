package com.bot.dhxy.ui;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainWindowIdentityParserWiringTest {

    @Test
    void uiUsesSharedWindowTitleIdentityParser() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/ui/MainWindowController.java"), StandardCharsets.UTF_8);

        assertTrue(source.contains("WindowTitleIdentityParser.parse(title)"));
        assertFalse(source.contains("WINDOW_IDENTITY_PATTERN"));
    }
}
