package com.bot.dhxy.task.xiuluo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class XiuluoSmartClickConfirmationWiringTest {

    private static final Path SOURCE = Path.of("src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java");

    public static void main(String[] args) throws IOException {
        String source = Files.readString(SOURCE);
        assertContains(source,
                "npcClickService.confirmPendingSmartClick(",
                "Xiuluo must commit runner-owned smart-click evidence after business proof");
        assertContains(source,
                "source + \":accept option consumed\"",
                "accept-task option consumption must confirm the pending accept-NPC click");
        assertContains(source,
                "source + \":under-five confirm consumed\"",
                "under-five confirm consumption must confirm the pending accept-NPC click");
        assertContains(source,
                "source + \":under-five wait consumed\"",
                "under-five wait consumption must confirm the pending accept-NPC click");
        assertContains(source,
                "\"DIALOG_TEMPLATE\", \"xiuluo-v2:enter-battle:\" + state.source() + \":option consumed\"",
                "normal enter-battle template consumption must confirm the pending target click");
        assertContains(source,
                "\"DIALOG_OCR\", \"xiuluo-v2:enter-battle-ocr:\" + state.source() + \":option consumed\"",
                "OCR recovered enter-battle consumption must confirm the pending target click");
        assertContains(source,
                "\"BATTLE_RADAR\", \"xiuluo-v2:\" + reason + \":combat radar confirmed\"",
                "direct-combat success must confirm the pending target click through battle-radar proof");
    }

    private static void assertContains(String source, String needle, String message) {
        if (!source.contains(needle)) {
            throw new AssertionError(message + " missing marker: " + needle);
        }
    }
}
