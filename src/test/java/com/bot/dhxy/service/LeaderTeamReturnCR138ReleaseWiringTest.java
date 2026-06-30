package com.bot.dhxy.service;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for leader-side CR138 TEAM_RETURN release wiring.
 */
public final class LeaderTeamReturnCR138ReleaseWiringTest {

    private LeaderTeamReturnCR138ReleaseWiringTest() {
    }

    public static void main(String[] args) throws Exception {
        requireLeaderReleaseWiring(
                "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java",
                "private WubeiStepOutcome runWaitTeamReturnPhase",
                "private boolean shouldYieldForTeamReturnSignal");
        requireLeaderReleaseWiring(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java",
                "private XiuluoStepOutcome waitTeamReturn",
                "private boolean shouldYieldForTeamReturnSignal");
    }

    private static void requireLeaderReleaseWiring(String path,
                                                   String methodMarker,
                                                   String nextMarker) throws Exception {
        String source = Files.readString(Path.of(path));
        int methodStart = source.indexOf(methodMarker);
        require(methodStart >= 0, path + " must keep the team-return wait phase");
        int methodEnd = source.indexOf(nextMarker, methodStart);
        require(methodEnd > methodStart, path + " team-return wait phase boundary must be readable");
        String method = source.substring(methodStart, methodEnd);

        require(method.contains("openLocalTeamReturnSupportWindow"),
                path + " must open local TEAM_RETURN release when leader yields for return signal");
        require(method.contains("closeLocalTeamReturnSupportWindow"),
                path + " must close local TEAM_RETURN release when return signal is gone/not needed");
        int open = method.indexOf("openLocalTeamReturnSupportWindow");
        int sharedState = method.indexOf("sharedState");
        require(open >= 0 && sharedState > open,
                path + " must open release before yielding shared state to member windows");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
