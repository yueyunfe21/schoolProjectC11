package com.bot.dhxy.service;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR138 local team-return release gating.
 */
public final class AutoBattleCR138TeamReturnReleaseWiringTest {

    private AutoBattleCR138TeamReturnReleaseWiringTest() {
    }

    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/task/AutoBattleTask.java"));

        int methodStart = source.indexOf("private boolean tryRunLocalTeamReturnRelease");
        require(methodStart >= 0, "AutoBattleTask must keep the local TEAM_RETURN release hook");
        int methodEnd = source.indexOf("/**\r\n     * @param context current window context.", methodStart);
        if (methodEnd < 0) {
            methodEnd = source.indexOf("/**\n     * @param context current window context.", methodStart);
        }
        require(methodEnd > methodStart, "local TEAM_RETURN release method boundary must be readable");
        String method = source.substring(methodStart, methodEnd);

        int gate = method.indexOf("TeamSupportCapability.TEAM_RETURN");
        int boxGate = method.indexOf("TeamSupportCapability.COMMON_BOX");
        int box = method.indexOf("commonBoxService.consumePendingBoxIfAllowed");
        int click = method.indexOf("teamReturnService.clickReturnTeamIfPresent");
        require(gate >= 0, "local member return-team click must require TEAM_RETURN capability");
        require(boxGate >= 0, "local common-box consume must require COMMON_BOX capability");
        require(box >= 0, "local TEAM_RETURN release must consume pending common-box first");
        require(click >= 0, "local TEAM_RETURN release must still use the existing return-team clicker");
        require(gate < click, "TEAM_RETURN capability check must happen before return-team click");
        require(boxGate < box, "COMMON_BOX capability check must happen before common-box consume");
        require(box < click, "common-box must be consumed before return-team click in the same release");

        int idleStart = source.indexOf("private void maybeRunIdleMaintenance");
        int idleEnd = source.indexOf("private boolean tryRunLocalTeamReturnRelease", idleStart);
        require(idleStart >= 0 && idleEnd > idleStart, "idle maintenance method boundary must be readable");
        String idle = source.substring(idleStart, idleEnd);
        require(idle.contains("tryRunLocalTeamReturnRelease(context)"),
                "idle maintenance must run the local release path before the legacy return-team click");
        require(idle.contains("!taskMaintenanceService.isLocalSupportMemberSession(context)"),
                "local support members must not use the legacy ungated return-team click path");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
