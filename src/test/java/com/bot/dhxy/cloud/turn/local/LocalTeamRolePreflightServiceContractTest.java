package com.bot.dhxy.cloud.turn.local;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalTeamRolePreflightServiceContractTest {

    @Test
    void absentMinimapMagnifierAfterAltTMeansSolo() {
        assertEquals(LocalTeamRolePreflightService.Role.SOLO,
                LocalTeamRolePreflightService.classifyPanel(false, false));
    }

    @Test
    void groupedPanelWithDismissButtonOrTransferLeaderButtonMeansLeader() {
        assertEquals(LocalTeamRolePreflightService.Role.LEADER,
                LocalTeamRolePreflightService.classifyPanel(true, true));
    }

    @Test
    void groupedPanelWithoutLeaderButtonsMeansMember() {
        assertEquals(LocalTeamRolePreflightService.Role.MEMBER,
                LocalTeamRolePreflightService.classifyPanel(true, false));
    }

    @Test
    void firstLocalLeaderMakesEveryOtherSelectedWindowAMember() {
        Map<String, LocalTeamRolePreflightService.Role> roles = LocalTeamRolePreflightService.assignGroupedRoles(
                List.of("window-1", "window-2", "window-3", "window-4", "window-5"), "window-3");

        assertEquals(LocalTeamRolePreflightService.Role.LEADER, roles.get("window-3"));
        assertEquals(LocalTeamRolePreflightService.Role.MEMBER, roles.get("window-1"));
        assertEquals(LocalTeamRolePreflightService.Role.MEMBER, roles.get("window-2"));
        assertEquals(LocalTeamRolePreflightService.Role.MEMBER, roles.get("window-4"));
        assertEquals(LocalTeamRolePreflightService.Role.MEMBER, roles.get("window-5"));
    }
}
