package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR77 修罗 start-exit prepath fire-and-handoff.
 *
 * <p>The 灵兽村出口 prepath is only a speed hint before the formal 修罗 target route. It may skip
 * movement proof, but normal current-map navigation must still prove movement before yielding.</p>
 */
public class NavigationXiuluoStartExitPrepathFireAndHandoffWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String navigation = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/service/NavigationService.java"), StandardCharsets.UTF_8);
        String xiuluo = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8);

        String startExitPrepath = between(xiuluo,
                "private XiuluoStepOutcome startLeavingStartMapIfPresent(",
                "private XiuluoStepOutcome navigateToTarget(");
        require(startExitPrepath.contains("xiuluo-v2:start-exit-prepath"),
                "修罗 start-exit prepath must keep its explicit source marker");
        require(!startExitPrepath.contains("navigationService.navigateToNPC("),
                "修罗 start-exit prepath must not enter cross-map navigateToMap before current-map exit click");
        require(startExitPrepath.contains("navigationService.navigateInCurrentMap("),
                "修罗 start-exit prepath must directly use current-map navigation once cached map is 灵兽村");
        require(startExitPrepath.contains(".source(\"xiuluo-v2:start-exit-prepath:currentMap\")"),
                "修罗 direct start-exit current-map click must keep the source required by the fire-and-handoff gate");
        require(startExitPrepath.contains("state.withStartExitPrepathStarted("),
                "修罗 start-exit prepath must continue into formal target navigation instead of parking");

        String navigateInCurrentMap = between(navigation,
                "public NavigationResult navigateInCurrentMap(",
                "    // =========================");
        require(navigateInCurrentMap.contains("isXiuluoStartExitPrepathFireAndHandoff(request)"),
                "current-map navigation must explicitly gate the CR77 fast path by 修罗 start-exit prepath");
        require(navigateInCurrentMap.contains("clickMiniMapPointForFireAndHandoff("),
                "CR77 fast path must use the fire-and-handoff mini-map click branch");
        require(navigateInCurrentMap.indexOf("clickMiniMapPointForFireAndHandoff(")
                        < navigateInCurrentMap.indexOf("clickMiniMapPointForHandoff("),
                "CR77 fast path must run before the normal movement-proof handoff branch");

        String gate = between(navigation,
                "private boolean isXiuluoStartExitPrepathFireAndHandoff(",
                "private MiniMapPathingAttemptResult clickMiniMapPointForFireAndHandoff(");
        require(gate.contains("\"xiuluo-v2:start-exit-prepath:currentMap\""),
                "fast path gate must accept the real current-map source produced by navigateToNPC");
        require(gate.contains("MAP_LING_SHOU_VILLAGE"),
                "fast path gate must require 灵兽村");
        require(gate.contains("request.getTargetX() == 11") && gate.contains("request.getTargetY() == 8"),
                "fast path gate must require the 灵兽村出口 coordinate (11,8)");

        String fireAndHandoff = between(navigation,
                "private MiniMapPathingAttemptResult clickMiniMapPointForFireAndHandoff(",
                "private MiniMapPathingAttemptResult clickMiniMapPointForHandoff(");
        require(!fireAndHandoff.contains("isMovingByPixelDiff("),
                "fire-and-handoff branch must not run fast-edge movement proof");
        require(!fireAndHandoff.contains("confirmMiniMapPathingStarted"),
                "fire-and-handoff branch must not run coordinate fallback movement proof");
        require(fireAndHandoff.contains("submitMiniMapClick("),
                "fire-and-handoff branch must still submit the mini-map coordinate click");
        require(fireAndHandoff.contains("closeMiniMapAfterFireAndHandoff("),
                "fire-and-handoff branch must close Alt+1 through the cheap close path");
        require(fireAndHandoff.contains("MiniMapPathingAttemptResult.PATHING_STARTED"),
                "fire-and-handoff branch must report PATHING_STARTED after successful click/close");

        String normalHandoff = between(navigation,
                "private MiniMapPathingAttemptResult clickMiniMapPointForHandoff(",
                "private MiniMapPathingAttemptResult confirmMiniMapPathingStartedForHandoff(");
        require(normalHandoff.contains("confirmMiniMapPathingStartedForHandoff("),
                "normal current-map handoff must still prove movement before PATHING_STARTED");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex);
        if (endIndex < 0) {
            throw new AssertionError("Missing source end marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
