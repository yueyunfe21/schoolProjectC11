package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for direct-combat target clicking.
 *
 * <p>Direct-combat is entered immediately after the normal stationary click pipeline has already
 * hidden player names. Pressing Alt+4 again would toggle the name layer back on and add OCR noise.</p>
 */
public class NpcClickDirectCombatNameLayerWiringTest {

    public static void main(String[] args) throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/bot/dhxy/service/NpcClickService.java"),
                StandardCharsets.UTF_8);

        String pipelineEntry = between(source,
                "private boolean runNpcClickPipeline(",
                "boolean lightScan = request.targetEvidence().equals(NpcTargetEvidence.TENTATIVE);");
        require(pipelineEntry.contains("if (directCombatClickMode)"),
                "direct-combat pipeline must explicitly skip the second Alt+4 name-layer toggle");
        require(pipelineEntry.contains("skip repeated Alt+4"),
                "direct-combat skip must document why the repeated toggle is unsafe");
        require(pipelineEntry.contains("} else if (!prepareNpcPipelineNameLayerOnce(request, verificationMode))"),
                "only non-direct-combat pipeline entry should press Alt+4");
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
