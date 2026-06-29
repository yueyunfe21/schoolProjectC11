package com.bot.dhxy.task.wubei;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR132 白龙马/显形镜 return-cache slot shifting.
 *
 * <p>Fresh runtime still needs a real WUBEI probe round. This guard protects the narrow source
 * wiring: probe rounds must learn the 显形镜 slot before the mirror is consumed, then use that
 * cached point as the return-item click point at RETURN_HOME. Ordinary combat keeps the original
 * return-item template cache.</p>
 */
public class WubeiCR132ProbeMirrorSlotReturnCacheWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String prescan = read(root, "src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java");
        String wubei = read(root, "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java");
        Path mirrorTemplate = root.resolve("images/template/bag/wubei_probe_item.png");

        mirrorTemplateExists(mirrorTemplate);
        prescanServiceHasImmediateTrackerGreenSlotLearn(prescan);
        wubeiProbeTrackerGreenLearnsMirrorSlot(wubei);
        wubeiProbeRuntimeBypassesReturnItemPathingPrescan(wubei);
        wubeiProbeRuntimeSkipsCombatPrescan(wubei);
        wubeiReturnHomeUsesProbeMirrorCacheKey(wubei);
        normalCombatStillUsesReturnItemCache(wubei);
    }

    private static void mirrorTemplateExists(Path mirrorTemplate) {
        require(Files.isRegularFile(mirrorTemplate),
                "CR132 probe repair requires existing 显形镜 template images/template/bag/wubei_probe_item.png");
    }

    private static void prescanServiceHasImmediateTrackerGreenSlotLearn(String prescan) {
        require(prescan.contains("public void afterTrackerGreenRequired("),
                "ReturnItemPrescanService must expose a narrow forced after-tracker-green slot learn API");
        String required = methodBody(prescan, "public void afterTrackerGreenRequired(");
        require(required.contains("Strategy.AFTER_TRACKER_GREEN"),
                "forced slot learn must use AFTER_TRACKER_GREEN instead of random strategy selection");
        require(required.contains("runPrescan(context, state, source + \":after-tracker-green\", false)"),
                "forced probe mirror slot learn must not schedule a later combat fallback for a disappearing item");

        String stateFor = methodBody(prescan, "private PrescanState stateFor(");
        require(stateFor.contains("forcedStrategy == null ? chooseStrategy(")
                        && stateFor.contains(": forcedStrategy"),
                "ReturnItemPrescanService must keep random selection for normal callers and forced strategy only for required slot learns");
    }

    private static void wubeiProbeTrackerGreenLearnsMirrorSlot(String wubei) {
        require(wubei.contains("PROBE_ITEM_TEMPLATE = \"bag/wubei_probe_item.png\""),
                "WubeiTask must keep the existing 显形镜 template constant");
        require(wubei.contains("RETURN_ITEM_TEMPLATE = \"bag/wubei_return_item.png\""),
                "WubeiTask must keep the normal return-item template constant");

        String click = methodBody(wubei, "private boolean clickTaskTrackerGreen(");
        String labelHelper = methodBody(wubei, "private boolean isProbeTrackerLabel(");
        require(labelHelper.contains("\"first-probe\".equals(label)")
                        && labelHelper.contains("\"second-probe\".equals(label)"),
                "only first-probe/second-probe tracker labels are allowed to force the 显形镜 slot learn");
        require(click.contains("isProbeTrackerLabel(label)"),
                "tracker-green click must branch on first-probe/second-probe labels for CR132");
        int probeBranch = click.indexOf("if (isProbeTrackerLabel(label))");
        require(probeBranch >= 0, "tracker-green click must have an explicit probe-label branch");
        String probeBlock = blockStartingAt(click, click.indexOf('{', probeBranch));
        require(probeBlock.contains("returnItemPrescanService.afterTrackerGreenRequired(")
                        && probeBlock.contains("PROBE_ITEM_TEMPLATE")
                        && probeBlock.contains("wubei:probe-mirror-slot:"),
                "probe tracker-green click must learn the 显形镜 slot with PROBE_ITEM_TEMPLATE");
        require(!probeBlock.contains("returnItemPrescanService.afterTrackerGreen("),
                "probe tracker-green click must not use the normal randomized return-item strategy");
        int normalBranch = click.indexOf("} else {", probeBranch);
        require(normalBranch >= 0, "tracker-green click must keep a normal return-item branch");
        String normalBlock = blockStartingAt(click, click.indexOf('{', normalBranch));
        require(normalBlock.contains("returnItemPrescanService.afterTrackerGreen(")
                        && normalBlock.contains("RETURN_ITEM_TEMPLATE"),
                "normal tracker-green click must still use the original return-item prescan");
        require(!normalBlock.contains("PROBE_ITEM_TEMPLATE"),
                "normal tracker-green click must not switch ordinary combat to the probe mirror template");
    }

    private static void wubeiReturnHomeUsesProbeMirrorCacheKey(String wubei) {
        String useReturnItem = methodBody(wubei, "private boolean useReturnItem(");
        require(useReturnItem.contains("String cachedTemplate = returnItemCacheTemplateForCurrentRuntime()"),
                "RETURN_HOME must choose the cache key from current WUBEI runtime");
        require(useReturnItem.contains("returnItemPrescanService.useCached(context, TASK_CODE, currentRoundNumber")
                        && useReturnItem.contains("cachedTemplate"),
                "RETURN_HOME must use the selected cache template when consuming a cached point");
        require(useReturnItem.contains("returnItemPrescanService.invalidate(context, TASK_CODE, currentRoundNumber, cachedTemplate"),
                "failed cached return must invalidate the selected cache template");
        require(useReturnItem.contains("returnItemPrescanService.completeRound(context, TASK_CODE, currentRoundNumber, cachedTemplate"),
                "verified cached return must clear the selected cache template");

        String templateHelper = methodBody(wubei, "private String returnItemCacheTemplateForCurrentRuntime()");
        require(templateHelper.contains("isProbeRuntimeActive() ? PROBE_ITEM_TEMPLATE : RETURN_ITEM_TEMPLATE"),
                "probe runtime must consume the mirror-slot cache; normal runtime must consume return-item cache");
    }

    private static void wubeiProbeRuntimeBypassesReturnItemPathingPrescan(String wubei) {
        String resolve = methodBody(wubei, "private WubeiStepOutcome runResolveAfterPathingPhase(");
        int probeBranch = resolve.indexOf("if (isProbeRuntimeActive())");
        int pathingPrescan = resolve.indexOf("returnItemPrescanService.whilePathing(");
        require(probeBranch >= 0 && pathingPrescan >= 0 && probeBranch < pathingPrescan,
                "probe runtime must resolve through the probe path before ordinary pathing return-item prescan");
        require(resolve.substring(probeBranch, pathingPrescan).contains("return resolveProbeAfterPathing(context, state)"),
                "probe runtime must bypass ordinary pathing return-item prescan and enter probe resolver");
    }

    private static void wubeiProbeRuntimeSkipsCombatPrescan(String wubei) {
        String battle = methodBody(wubei, "private WubeiStepOutcome tickWaitBattleFinish(");
        int normalCombatBranch = battle.indexOf("if (!isProbeRuntimeActive())");
        require(normalCombatBranch >= 0,
                "WAIT_BATTLE_FINISH combat prescan must be guarded by !isProbeRuntimeActive()");

        String normalCombatBlock = blockStartingAt(battle, battle.indexOf('{', normalCombatBranch));
        require(normalCombatBlock.contains("returnItemPrescanService.whileInCombat(")
                        && normalCombatBlock.contains("RETURN_ITEM_TEMPLATE")
                        && !normalCombatBlock.contains("PROBE_ITEM_TEMPLATE"),
                "ordinary WUBEI combat must keep return-item combat prescan with RETURN_ITEM_TEMPLATE only");

        int probeElse = battle.indexOf("} else {", normalCombatBranch);
        require(probeElse >= 0,
                "WAIT_BATTLE_FINISH must keep an explicit probe-runtime else branch");
        String probeCombatBlock = blockStartingAt(battle, battle.indexOf('{', probeElse));
        require(probeCombatBlock.contains(
                        "return-item combat prescan skipped for probe runtime; mirror slot cache should already exist"),
                "probe runtime must log that combat prescan is skipped and mirror slot cache should already exist");
        require(!probeCombatBlock.contains("returnItemPrescanService.whileInCombat("),
                "probe runtime must not call returnItemPrescanService.whileInCombat(...) after the mirror is consumed");
    }

    private static void normalCombatStillUsesReturnItemCache(String wubei) {
        String battle = methodBody(wubei, "private WubeiStepOutcome tickWaitBattleFinish(");
        require(battle.contains("!isProbeRuntimeActive()")
                        && battle.contains("returnItemPrescanService.whileInCombat(")
                        && battle.contains("RETURN_ITEM_TEMPLATE"),
                "normal combat must still keep CR132 return-item combat prescan while probe runtime skips it");
    }

    private static String read(Path root, String relativePath) throws Exception {
        return Files.readString(root.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static String methodBody(String source, String signature) {
        int signatureIndex = source.indexOf(signature);
        if (signatureIndex < 0) {
            throw new AssertionError("Missing source marker: " + signature);
        }
        int bodyStart = source.indexOf('{', signatureIndex);
        if (bodyStart < 0) {
            throw new AssertionError("Missing method body for: " + signature);
        }
        int depth = 0;
        for (int i = bodyStart; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(bodyStart, i + 1);
                }
            }
        }
        throw new AssertionError("Unclosed method body for: " + signature);
    }

    private static String blockStartingAt(String source, int bodyStart) {
        if (bodyStart < 0 || bodyStart >= source.length() || source.charAt(bodyStart) != '{') {
            throw new AssertionError("Missing block body at index: " + bodyStart);
        }
        int depth = 0;
        for (int i = bodyStart; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(bodyStart, i + 1);
                }
            }
        }
        throw new AssertionError("Unclosed block body at index: " + bodyStart);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
