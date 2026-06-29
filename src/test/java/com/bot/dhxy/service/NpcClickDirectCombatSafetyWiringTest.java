package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR73 direct-combat safety.
 */
public class NpcClickDirectCombatSafetyWiringTest {

    public static void main(String[] args) throws Exception {
        String npcClick = read("src/main/java/com/bot/dhxy/service/NpcClickService.java");
        String xiuluo = read("src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java");
        String wubei = read("src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java");

        require(npcClick.contains("public DirectCombatClickResult tryDirectCombatTargetClick("),
                "direct-combat must return a result that can mark position refresh");
        require(npcClick.contains("GameStateUtil.FlyingState flyingState = gameStateUtil.detectFlyingState("),
                "direct-combat must probe mount/flying state before Alt+A");
        require(npcClick.contains("flyingState == GameStateUtil.FlyingState.FLYING"),
                "confirmed flying direct-combat must run the dismount branch");
        require(npcClick.contains("InputAction.pressAltC()"),
                "confirmed flying direct-combat must submit Alt+C before Alt+A");
        require(npcClick.contains("DirectCombatClickResult.positionRefreshRequired("),
                "failed direct-combat must report that target position may be stale");

        require(xiuluo.contains("directCombat.positionRefreshRequired()"),
                "Xiuluo must branch on direct-combat position-refresh-required");
        require(xiuluo.contains("XiuluoPhase.NAVIGATE_TO_TARGET"),
                "Xiuluo refresh branch must rerun target navigation/current-map approach");

        require(wubei.contains("DirectCombatClickResult enteredCombat"),
                "Wubei direct-combat fallback must consume the structured result");
        require(wubei.contains("markTrackerRetryAfterDirectCombatDisplacement("),
                "Wubei direct-combat failure must force tracker/path refresh before retry");
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
