package com.bot.dhxy.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UICleanerServiceTemplateCatalogTest {

    @Test
    void genericCleanupDiscoversEveryCurrentCancelTemplateAsset() {
        List<String> templates = UICleanerService.genericCloseButtonTemplates();

        assertTrue(templates.stream().anyMatch(path -> path.endsWith("x.png")));
        assertTrue(templates.stream().anyMatch(path -> path.endsWith("x1.png")));
        assertTrue(templates.stream().anyMatch(path -> path.endsWith("x7.png")));
        assertTrue(templates.stream().anyMatch(path -> path.endsWith("npc_busy_cancel.png")));
        assertTrue(templates.size() >= 9);
    }
}
