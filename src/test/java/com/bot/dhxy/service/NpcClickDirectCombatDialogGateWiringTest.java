package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR82 direct-combat dialog gating.
 *
 * <p>After Alt+A has entered target-pick mode, direct-combat must not run the normal NPC pre-click
 * dialog detection/cleanup gate. Normal dialog-mode NPC clicks still keep that safety gate.</p>
 */
public class NpcClickDirectCombatDialogGateWiringTest {

    public static void main(String[] args) throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/bot/dhxy/service/NpcClickService.java"),
                StandardCharsets.UTF_8);

        String pipeline = between(source,
                "private boolean runNpcClickPipeline(",
                "private boolean prepareNpcPipelineNameLayerOnce(");

        int skipLog = pipeline.indexOf("skips pre-click dialog gate in direct-combat mode");
        int normalGate = pipeline.indexOf("dialogType = currentPreClickDialogType(request, \"before-learned-memory\")");
        require(skipLog >= 0, "CR82 must explicitly skip the pre-click dialog gate in direct-combat mode");
        require(normalGate > skipLog, "normal pre-click dialog gate must remain outside the direct-combat branch");

        String directCombatBranch = pipeline.substring(skipLog, normalGate);
        require(!directCombatBranch.contains("currentPreClickDialogType("),
                "direct-combat branch must not call pre-click dialog type detection");
        require(!directCombatBranch.contains("detectDialogTypeNoFocus("),
                "direct-combat branch must not run fallback dialog detection");
        require(!directCombatBranch.contains("handleDialog("),
                "direct-combat branch must not run dialog cleanup before target scanning");

        require(pipeline.contains("if (!directCombatClickMode) {\n                dialogType = dialogService.detectDialogTypeNoFocus(\"after-tooltip\", false, 0);"),
                "post-tooltip dialog recheck must also be disabled for direct-combat mode");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex);
        if (endIndex < 0) {
            throw new AssertionError("Missing source marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
