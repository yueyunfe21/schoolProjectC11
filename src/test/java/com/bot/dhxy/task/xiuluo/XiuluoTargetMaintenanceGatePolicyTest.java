package com.bot.dhxy.task.xiuluo;

import com.bot.dhxy.model.navigation.NavigationResult;

public class XiuluoTargetMaintenanceGatePolicyTest {

    public static void main(String[] args) {
        assertGate("stale active watcher pathing must not open summon-skill window",
                false,
                NavigationResult.pathingStarted("recent window pathing still active; observer will confirm map"));
        assertGate("current-map target walk must not open summon-skill window",
                false,
                NavigationResult.pathingStarted("current-map mini-map click started pathing"));
        assertGate("fresh world-map route click opens summon-skill window",
                true,
                NavigationResult.pathingStarted("world-map route clicked"));
        assertGate("prepared route dialog click opens summon-skill window",
                true,
                NavigationResult.pathingStarted("route dialog clicked before pathing guard; observer will confirm pathing"));
        assertGate("same target pending route opens summon-skill window",
                true,
                NavigationResult.pathingStarted("same target route already submitted before world-map search; watcher will confirm pathing"));
    }

    private static void assertGate(String caseName,
                                   boolean expected,
                                   NavigationResult result) {
        boolean actual = XiuluoTaskV2.shouldOpenTeamPathingMaintenanceWindowAfterTargetNavigation(result);
        if (actual != expected) {
            throw new AssertionError(caseName + ": expected=" + expected + " actual=" + actual);
        }
    }
}
