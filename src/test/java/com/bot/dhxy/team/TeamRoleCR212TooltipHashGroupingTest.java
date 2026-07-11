package com.bot.dhxy.team;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionMetricsService;
import com.bot.dhxy.cloud.decision.CloudDecisionProperties;
import com.bot.dhxy.cloud.task.TeamRoleTooltipCloudDecision;
import com.bot.dhxy.cloud.task.TeamRoleTooltipCloudDecisionService;
import com.bot.dhxy.cloud.task.TeamRoleTooltipCloudRequest;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TeamRoleCR212TooltipHashGroupingTest {

    public static void main(String[] args) throws Exception {
        runTests(TeamRoleCR212TooltipHashGroupingTest.class);
        System.out.println("TeamRoleCR212TooltipHashGroupingTest passed");
    }

    @Test
    public void junitRunsMainSuite() throws Exception {
        main(new String[0]);
    }

    public void testSameSessionAndHashUsesOneCloudDecisionAndDerivesLocalRoles() {
        RecordingTooltipCloudService cloud = new RecordingTooltipCloudService("10001");
        TeamRoleDetectionService service = new TeamRoleDetectionService(
                null, null, null, null, null, null, null,
                new WindowTaskContextHolder(new WindowIsolationProperties()),
                null, null, null, null, cloud);

        TeamRoleDetectionService.TeamRoleDetectionResult leader =
                service.detectGroupedRoleFromCloudTooltipRequest(
                        context("session-212", "leader-window", 1L),
                        request("leader-window", "10001", "same-hash"));
        TeamRoleDetectionService.TeamRoleDetectionResult member =
                service.detectGroupedRoleFromCloudTooltipRequest(
                        context("session-212", "member-window", 2L),
                        request("member-window", "20002", "same-hash"));

        require(leader.role() == TeamRoleStatus.LEADER, "leader role must be derived from leaderClientId");
        require(member.role() == TeamRoleStatus.MEMBER, "member role must be derived locally from cached leaderClientId");
        require(cloud.calls == 1, "same local-team session and image hash must call TEAM_ROLE_TOOLTIP once");
        require("same-hash".equals(member.tooltipGroupEvidence().groupHash()), "evidence must keep group hash");
        require("10001".equals(member.tooltipGroupEvidence().leaderPlayerId()), "evidence must keep cloud leaderClientId");

        service.detectGroupedRoleFromCloudTooltipRequest(
                context("session-212", "third-window", 3L),
                request("third-window", "30003", "different-hash"));
        require(cloud.calls == 2, "different strict hash must trigger a separate cloud decision");
    }

    public void testSameHashInDifferentSessionsDoesNotReuseCloudDecision() {
        RecordingTooltipCloudService cloud = new RecordingTooltipCloudService("10001");
        TeamRoleDetectionService service = new TeamRoleDetectionService(
                null, null, null, null, null, null, null,
                new WindowTaskContextHolder(new WindowIsolationProperties()),
                null, null, null, null, cloud);

        service.detectGroupedRoleFromCloudTooltipRequest(
                context("session-a", "leader-window-a", 11L),
                request("leader-window-a", "10001", "same-hash"));
        service.detectGroupedRoleFromCloudTooltipRequest(
                context("session-b", "leader-window-b", 12L),
                request("leader-window-b", "10001", "same-hash"));

        require(cloud.calls == 2, "same strict hash in different local-team sessions must execute cloud separately");
    }

    private static TaskExecutionContext context(String session, String windowId, long runId) {
        return TaskExecutionContext.builder()
                .taskCode("startup")
                .windowId(windowId)
                .windowRole("UNKNOWN")
                .localTeamSessionKey(session)
                .localLeaderPresent(true)
                .taskRunId(runId)
                .nativeWindowWidth(1024)
                .nativeWindowHeight(768)
                .build();
    }

    private static TeamRoleTooltipCloudRequest request(String windowId, String playerId, String hash) {
        return TeamRoleTooltipCloudRequest.builder()
                .imagePayloadBase64("payload-" + hash)
                .payloadMimeType("image/png")
                .imageSha256(hash)
                .rawImagePath("images/temp/" + hash + ".png")
                .debugImageId("team-role-tooltip:" + playerId)
                .currentPlayerId(playerId)
                .windowId(windowId)
                .taskCode("startup")
                .phase("tooltip-id")
                .taskRunId("1")
                .policyVersion("test")
                .windowWidth(1024)
                .windowHeight(768)
                .build();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void runTests(Class<?> testClass) throws Exception {
        Object test = testClass.getDeclaredConstructor().newInstance();
        for (Method method : testClass.getDeclaredMethods()) {
            if (!method.getName().startsWith("test")) {
                continue;
            }
            try {
                method.invoke(test);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception exception) {
                    throw exception;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw e;
            }
        }
    }

    private static final class RecordingTooltipCloudService extends TeamRoleTooltipCloudDecisionService {
        private final String leaderClientId;
        private int calls;

        private RecordingTooltipCloudService(String leaderClientId) {
            super(new CloudDecisionCoordinator(
                    new CloudDecisionProperties(),
                    request -> {
                        throw new AssertionError("fake cloud service overrides detect");
                    },
                    new CloudDecisionMetricsService()));
            this.leaderClientId = leaderClientId;
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public TeamRoleTooltipCloudDecision detect(TeamRoleTooltipCloudRequest request) {
            calls++;
            return TeamRoleTooltipCloudDecision.builder()
                    .status(TeamRoleTooltipCloudDecision.Status.CLOUD_FOUND)
                    .role(leaderClientId.equals(request.getCurrentPlayerId())
                            ? TeamRoleTooltipCloudDecision.Role.LEADER
                            : TeamRoleTooltipCloudDecision.Role.MEMBER)
                    .leaderClientId(leaderClientId)
                    .currentPlayerId(request.getCurrentPlayerId())
                    .reason("test")
                    .build();
        }
    }
}
