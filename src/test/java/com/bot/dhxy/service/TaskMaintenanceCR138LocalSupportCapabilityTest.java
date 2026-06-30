package com.bot.dhxy.service;

import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.maintenance.TeamSupportCapability;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;

import java.util.List;

/**
 * CR138 behavior guard for local support capability gates.
 */
public final class TaskMaintenanceCR138LocalSupportCapabilityTest {

    private TaskMaintenanceCR138LocalSupportCapabilityTest() {
    }

    public static void main(String[] args) {
        WindowTaskContextHolder holder = new WindowTaskContextHolder(new WindowIsolationProperties());
        TaskMaintenanceService service = new TaskMaintenanceService(
                null, new GameContext(), null, null, holder);

        String staleSession = "local-session-stale-leader";
        TaskExecutionContext staleMemberBeforeLeader = TaskExecutionContext.builder()
                .taskCode("auto_battle")
                .requestedTaskCode("wubei")
                .windowId("member-window-stale")
                .windowRole("MEMBER")
                .localTeamSessionKey(staleSession)
                .localLeaderWindowId("stale-leader-window")
                .localLeaderPresent(true)
                .localSupportMember(true)
                .taskRunId(137L)
                .build();
        service.registerLocalTeamSessionCandidate(
                staleSession,
                List.of("stale-leader-window", "member-window-stale"),
                "cr138-test:stale-leader-candidate");
        require(!service.isLocalSupportMemberSession(staleMemberBeforeLeader),
                "stale submit-time leader id must not count as a live-detected local leader");
        require(service.isPendingLocalSupportLeaderDetection(staleMemberBeforeLeader),
                "stale submit-time leader id should keep members pending until live role evidence resolves");

        TaskExecutionContext staleLeaderCandidate = TaskExecutionContext.builder()
                .taskCode("auto_battle")
                .requestedTaskCode("wubei")
                .windowId("stale-leader-window")
                .windowRole("MEMBER")
                .localTeamSessionKey(staleSession)
                .localLeaderWindowId("stale-leader-window")
                .localLeaderPresent(true)
                .localSupportMember(true)
                .taskRunId(136L)
                .build();
        service.markLocalTeamWindowRoleDetected(
                staleLeaderCandidate, "stale-leader-window", "MEMBER", "cr138-test:stale-leader-not-leader");
        service.markLocalTeamWindowRoleDetected(
                staleMemberBeforeLeader, "member-window-stale", "MEMBER", "cr138-test:member-not-leader");
        require(!service.isPendingLocalSupportLeaderDetection(staleMemberBeforeLeader),
                "all candidates detected as non-leader should end pending leader detection");
        require(!service.isLocalSupportMemberSession(staleMemberBeforeLeader),
                "leader-absent sessions must not become local support sessions");

        String partialSubmitSession = "local-session-partial-submit";
        TaskExecutionContext partialSubmitMember = TaskExecutionContext.builder()
                .taskCode("auto_battle")
                .requestedTaskCode("wubei")
                .windowId("partial-submit-member")
                .windowRole("MEMBER")
                .localTeamSessionKey(partialSubmitSession)
                .localLeaderWindowId(null)
                .localLeaderPresent(true)
                .localSupportMember(true)
                .taskRunId(135L)
                .build();
        service.registerLocalTeamSessionCandidate(
                partialSubmitSession,
                List.of("partial-submit-member", "partial-submit-failed"),
                "cr138-test:partial-submit-candidates");
        service.completeLocalTeamSessionWindow(
                partialSubmitSession,
                "partial-submit-failed",
                "cr138-test:partial-submit-failed");
        require(service.isPendingLocalSupportLeaderDetection(partialSubmitMember),
                "one submit-failed candidate alone must not end pending leader detection");
        service.markLocalTeamWindowRoleDetected(
                partialSubmitMember,
                "partial-submit-member",
                "MEMBER",
                "cr138-test:partial-submit-member-not-leader");
        require(!service.isPendingLocalSupportLeaderDetection(partialSubmitMember),
                "submit-failed candidates must count as resolved when confirming no local leader");
        require(!service.isLocalSupportMemberSession(partialSubmitMember),
                "partial-submit leader-absent sessions must not become local support sessions");

        WindowRuntimeContext leaderWindow = new WindowRuntimeContext("leader-window", new GameContext());
        leaderWindow.setRole(WindowRole.LEADER);
        holder.bind(leaderWindow);

        TaskExecutionContext leader = TaskExecutionContext.builder()
                .taskCode("xiuluo_v2")
                .requestedTaskCode("xiuluo_v2")
                .windowId("leader-window")
                .windowRole("LEADER")
                .localTeamSessionKey("local-session-1")
                .localLeaderWindowId("leader-window")
                .localLeaderPresent(true)
                .taskRunId(138L)
                .build();
        service.markLocalTeamLeaderDetected(leader, "leader-window", "cr138-test:live-leader");

        service.beginTeamMaintenanceRound(leader, "xiuluo_v2", 7, "cr138-test");
        service.openTeamFirstAidMaintenanceWindow(leader, "xiuluo_v2", 7, "cr138-test");

        TaskExecutionContext staleMember = TaskExecutionContext.builder()
                .taskCode("auto_battle")
                .requestedTaskCode("wubei")
                .windowId("member-window")
                .windowRole("MEMBER")
                .localTeamSessionKey("local-session-1")
                .localLeaderWindowId("leader-window")
                .localLeaderPresent(true)
                .localSupportMember(true)
                .taskRunId(139L)
                .build();

        require(!service.awaitTeamFirstAidMaintenanceWindowOpen(staleMember, "wubei", 0L),
                "old requested=wubei gate should stay closed in this regression setup");
        require(service.awaitLocalTeamSupportCapabilityOpen(
                        staleMember, TeamSupportCapability.FIRST_AID, 0L),
                "local session FIRST_AID capability must open even when member requestedTaskCode is stale");
        require(!service.awaitLocalTeamSupportCapabilityOpen(
                        staleMember, TeamSupportCapability.COMMON_BOX, 0L),
                "COMMON_BOX must not be implied by FIRST_AID");
        require(!service.awaitLocalTeamSupportCapabilityOpen(
                        staleMember, TeamSupportCapability.TEAM_RETURN, 0L),
                "TEAM_RETURN must not be implied by FIRST_AID");
        require(!service.awaitLocalTeamSupportCapabilityOpen(
                        staleMember, TeamSupportCapability.SUMMON_SKILL, 0L),
                "SUMMON_SKILL must not be implied by FIRST_AID");
        require(!service.awaitLocalTeamSupportCapabilityOpen(
                        staleMember, TeamSupportCapability.LEFT_TOP_STATUS, 0L),
                "LEFT_TOP_STATUS must not be implied by FIRST_AID");

        service.closeTeamMaintenanceWindow(leader, "xiuluo_v2", 7, "cr138-test:first-aid-closed");
        service.beginTeamMaintenanceRound(leader, "xiuluo_v2", 8, "cr138-test:pathing");
        service.openTeamPathingMaintenanceWindow(leader, "xiuluo_v2", 8, "cr138-test:pathing");
        require(service.awaitLocalTeamSupportCapabilityOpen(
                        staleMember, TeamSupportCapability.PATHING_WINDOW, 0L),
                "PATHING_WINDOW must open on explicit leader pathing release");
        require(service.awaitLocalTeamSupportCapabilityOpen(
                        staleMember, TeamSupportCapability.COMMON_BOX, 0L),
                "COMMON_BOX must open only for a wider explicit release window");
        require(service.awaitLocalTeamSupportCapabilityOpen(
                        staleMember, TeamSupportCapability.SUMMON_SKILL, 0L),
                "SUMMON_SKILL must open only for a wider explicit pathing release window");
        require(service.awaitLocalTeamSupportCapabilityOpen(
                        staleMember, TeamSupportCapability.LEFT_TOP_STATUS, 0L),
                "LEFT_TOP_STATUS must open only for a wider explicit pathing release window");
        service.closeTeamMaintenanceWindow(leader, "xiuluo_v2", 8, "cr138-test:pathing-closed");
        require(!service.awaitLocalTeamSupportCapabilityOpen(
                        staleMember, TeamSupportCapability.COMMON_BOX, 0L),
                "COMMON_BOX must close with the pathing maintenance window");
        require(!service.awaitLocalTeamSupportCapabilityOpen(
                        staleMember, TeamSupportCapability.SUMMON_SKILL, 0L),
                "SUMMON_SKILL must close with the pathing maintenance window");
        require(!service.awaitLocalTeamSupportCapabilityOpen(
                        staleMember, TeamSupportCapability.LEFT_TOP_STATUS, 0L),
                "LEFT_TOP_STATUS must close with the pathing maintenance window");

        service.openLocalTeamReturnSupportWindow(leader, "cr138-test:return-release");
        require(service.awaitLocalTeamSupportCapabilityOpen(
                        staleMember, TeamSupportCapability.TEAM_RETURN, 0L),
                "local session TEAM_RETURN capability must open only on explicit leader release");
        require(service.awaitLocalTeamSupportCapabilityOpen(
                        staleMember, TeamSupportCapability.COMMON_BOX, 0L),
                "local session return release must also permit common-box before return-team");
        service.closeLocalTeamReturnSupportWindow(leader, "cr138-test:return-release-closed");
        require(!service.awaitLocalTeamSupportCapabilityOpen(
                        staleMember, TeamSupportCapability.TEAM_RETURN, 0L),
                "local session TEAM_RETURN capability must close after the leader signal is gone");
        require(!service.awaitLocalTeamSupportCapabilityOpen(
                        staleMember, TeamSupportCapability.COMMON_BOX, 0L),
                "local session COMMON_BOX must close after the return release is gone");

        String cleanupSession = "local-session-cleanup";
        TaskExecutionContext cleanupLeader = TaskExecutionContext.builder()
                .taskCode("xiuluo_v2")
                .requestedTaskCode("xiuluo_v2")
                .windowId("cleanup-leader")
                .windowRole("LEADER")
                .localTeamSessionKey(cleanupSession)
                .localLeaderWindowId("cleanup-leader")
                .localLeaderPresent(true)
                .taskRunId(140L)
                .build();
        TaskExecutionContext cleanupMember = TaskExecutionContext.builder()
                .taskCode("auto_battle")
                .requestedTaskCode("wubei")
                .windowId("cleanup-member")
                .windowRole("MEMBER")
                .localTeamSessionKey(cleanupSession)
                .localLeaderWindowId("cleanup-leader")
                .localLeaderPresent(true)
                .localSupportMember(true)
                .taskRunId(141L)
                .build();
        service.registerLocalTeamSessionCandidate(
                cleanupSession,
                List.of("cleanup-leader", "cleanup-member"),
                "cr138-test:cleanup-candidates");
        service.markLocalTeamLeaderDetected(cleanupLeader, "cleanup-leader", "cr138-test:cleanup-live-leader");
        service.openTeamPathingMaintenanceWindow(cleanupLeader, "xiuluo_v2", 9, "cr138-test:cleanup-pathing");
        require(service.awaitLocalTeamSupportCapabilityOpen(
                        cleanupMember, TeamSupportCapability.PATHING_WINDOW, 0L),
                "cleanup session should expose capability before all windows finish");

        service.completeLocalTeamSessionWindow(cleanupSession, "cleanup-leader", "cr138-test:cleanup-leader-done");
        require(service.isLocalSupportMemberSession(cleanupMember),
                "one finished window must not clear a local-team session while another candidate is still active");
        require(service.awaitLocalTeamSupportCapabilityOpen(
                        cleanupMember, TeamSupportCapability.PATHING_WINDOW, 0L),
                "one finished window must not clear capabilities for remaining local support members");

        service.completeLocalTeamSessionWindow(cleanupSession, "cleanup-member", "cr138-test:cleanup-member-done");
        require(!service.isLocalSupportMemberSession(cleanupMember),
                "local-team session cleanup must clear live leader evidence after every candidate window finishes");
        require(!service.isPendingLocalSupportLeaderDetection(cleanupMember),
                "local-team session cleanup must clear pending candidate state after every candidate window finishes");
        require(!service.awaitLocalTeamSupportCapabilityOpen(
                        cleanupMember, TeamSupportCapability.PATHING_WINDOW, 0L),
                "local-team session cleanup must clear capability state after every candidate window finishes");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
