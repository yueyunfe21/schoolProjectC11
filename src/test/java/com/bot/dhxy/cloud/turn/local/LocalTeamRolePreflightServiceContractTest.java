package com.bot.dhxy.cloud.turn.local;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

    @Test
    void rawArgbHashIsStableAndChangesWithAnyAnchorPixel() {
        BufferedImage first = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        BufferedImage same = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        BufferedImage different = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        first.setRGB(4, 6, 0xff123456);
        same.setRGB(4, 6, 0xff123456);
        different.setRGB(4, 6, 0xff123457);

        assertEquals(LocalTeamRolePreflightService.rawArgbSha256(first),
                LocalTeamRolePreflightService.rawArgbSha256(same));
        assertNotEquals(LocalTeamRolePreflightService.rawArgbSha256(first),
                LocalTeamRolePreflightService.rawArgbSha256(different));
    }

    @Test
    void onlyMembersSharingAConfirmedLeaderAnchorReceiveTheGroupHash() {
        Map<String, LocalTeamRolePreflightService.Preflight> roles = Map.of(
                "leader-a", new LocalTeamRolePreflightService.Preflight(
                        "leader-a", LocalTeamRolePreflightService.Role.LEADER, null, false, null),
                "member-a", new LocalTeamRolePreflightService.Preflight(
                        "member-a", LocalTeamRolePreflightService.Role.MEMBER, null, false, null),
                "member-b", new LocalTeamRolePreflightService.Preflight(
                        "member-b", LocalTeamRolePreflightService.Role.MEMBER, null, false, null));

        Map<String, LocalTeamRolePreflightService.Preflight> grouped =
                LocalTeamRolePreflightService.applyConfirmedLeaderAnchorGroups(roles, Map.of(
                        "leader-a", "anchor-team-a",
                        "member-a", "anchor-team-a",
                        "member-b", "anchor-team-b"));

        assertEquals("anchor-team-a", grouped.get("leader-a").groupHash());
        assertEquals("anchor-team-a", grouped.get("member-a").groupHash());
        assertEquals(null, grouped.get("member-b").groupHash());
    }
}
