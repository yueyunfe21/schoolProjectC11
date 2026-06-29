package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR76 NPC learned-memory ordering.
 *
 * <p>This is a wiring guard because the behavior depends on private strategy order and live
 * window/input collaborators. It prevents the fast path from drifting back behind Alt+4, and keeps
 * 五倍/白龙马 combat-target ordering outside the optimization.</p>
 */
public class NpcClickLearnedMemoryFastPathWiringTest {

    public static void main(String[] args) throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/bot/dhxy/service/NpcClickService.java"),
                StandardCharsets.UTF_8);

        String pipeline = between(source,
                "private boolean runNpcClickPipeline(",
                "private boolean prepareNpcPipelineNameLayerOnce(");

        int earlyDialog = pipeline.indexOf("currentPreClickDialogType(request, \"before-early-learned-memory\")");
        int earlyLearned = pipeline.indexOf("tryLearnedMemoryStrategy(request, verifier, pipelineState)");
        int nameLayer = pipeline.indexOf("prepareNpcPipelineNameLayerOnce(request, verificationMode)");
        require(earlyDialog >= 0, "CR76 must keep pre-click dialog safety before early learned memory");
        require(earlyLearned > earlyDialog, "early learned-memory click must run after pre-click dialog safety");
        require(nameLayer > earlyLearned, "early learned-memory click must run before Alt+4 name-layer preparation");

        String earlyGate = between(pipeline,
                "boolean canTryLearnedMemoryBeforeNameLayer",
                "if (directCombatClickMode)");
        require(earlyGate.contains("!request.sourceTask().equals(TaskType.WUBEI)"),
                "CR76 fast path must not reorder WUBEI/白龙马 probe target semantics");
        require(earlyGate.contains("request.targetRole() != NpcRole.COMBAT_TARGET"),
                "CR76 fast path must not run before Alt+4 for combat targets");

        String postPrepare = pipeline.substring(nameLayer);
        require(postPrepare.contains("!learnedMemoryTriedBeforeNameLayer"),
                "learned memory must not click the same remembered point twice in one pipeline");
        int wubeiTooltip = postPrepare.indexOf("if (request.sourceTask().equals(TaskType.WUBEI))");
        int postLearned = postPrepare.indexOf("tryLearnedMemoryStrategy(request, verifier, pipelineState)");
        require(wubeiTooltip >= 0 && postLearned > wubeiTooltip,
                "WUBEI tooltip-first path must remain before post-preparation learned memory");
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
