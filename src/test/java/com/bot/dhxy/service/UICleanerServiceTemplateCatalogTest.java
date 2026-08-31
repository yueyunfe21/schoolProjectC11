package com.bot.dhxy.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UICleanerServiceTemplateCatalogTest {

    @Test
    void genericCleanupDiscoversEveryXButtonSkin() {
        List<String> templates = UICleanerService.genericCloseButtonTemplates();

        assertTrue(templates.stream().anyMatch(path -> path.endsWith("x.png")));
        assertTrue(templates.stream().anyMatch(path -> path.endsWith("x1.png")));
        assertTrue(templates.stream().anyMatch(path -> path.endsWith("x7.png")));
        assertTrue(templates.size() >= 9);
    }

    @Test
    void genericCleanupUsesQuxiaoOnlyAfterEveryXSkin() {
        List<String> templates = UICleanerService.genericCloseButtonTemplates();

        assertEquals("quxiao.png", templates.get(templates.size() - 1).replace('\\', '/').replaceAll("^.*/", ""),
                "quxiao.png must be the final fallback after all X skins");
        assertTrue(templates.stream().noneMatch(path -> path.endsWith("npc_busy_cancel.png")),
                "npc_busy_cancel.png must stay out of the generic pool");
        assertTrue(templates.subList(0, templates.size() - 1).stream().allMatch(
                        path -> path.replace('\\', '/').matches(".*/x\\d*\\.png")),
                "every template before quxiao.png must be an X skin: " + templates);
    }
}
