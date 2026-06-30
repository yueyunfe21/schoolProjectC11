package com.bot.dhxy.service;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for the CR138 first-aid migration slice.
 */
public final class AutoCombatCR138FirstAidGateWiringTest {

    private AutoCombatCR138FirstAidGateWiringTest() {
    }

    public static void main(String[] args) throws Exception {
        String autoCombat = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/service/AutoCombatService.java"));

        int methodStart = autoCombat.indexOf("private boolean runPendingFollowerFirstAidIfAllowed");
        require(methodStart >= 0, "AutoCombatService must keep the pending follower first-aid hook");
        int methodEnd = autoCombat.indexOf("private boolean shouldDeferFollowerFirstAid", methodStart);
        require(methodEnd > methodStart, "pending follower first-aid method boundary must be readable");
        String method = autoCombat.substring(methodStart, methodEnd);

        int localGate = method.indexOf("awaitLocalTeamSupportCapabilityOpen");
        int oldGate = method.indexOf("awaitTeamFirstAidMaintenanceWindowOpen");
        require(localGate >= 0, "CR138 first slice must check local support FIRST_AID capability");
        require(oldGate >= 0, "old task-key first-aid gate must remain as fallback for non-session windows");
        require(localGate < oldGate,
                "local support FIRST_AID capability must be tried before stale requestedTaskCode gate");
        require(method.contains("TeamSupportCapability.FIRST_AID"),
                "local gate must be capability-specific and only for FIRST_AID");
        require(method.contains("gate=local-team capability=FIRST_AID"),
                "logs must show when pending first-aid uses the local-team FIRST_AID gate");
        require(!method.contains("consumePendingBoxIfAllowed"),
                "FIRST_AID_ONLY handling must not consume common box inside the first-aid branch");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
