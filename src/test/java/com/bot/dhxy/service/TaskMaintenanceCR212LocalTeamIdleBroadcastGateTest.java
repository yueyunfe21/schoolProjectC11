package com.bot.dhxy.service;

import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class TaskMaintenanceCR212LocalTeamIdleBroadcastGateTest {

    public static void main(String[] args) throws Exception {
        runTests(TaskMaintenanceCR212LocalTeamIdleBroadcastGateTest.class);
        System.out.println("TaskMaintenanceCR212LocalTeamIdleBroadcastGateTest passed");
    }

    @Test
    public void junitRunsMainSuite() throws Exception {
        main(new String[0]);
    }

    public void testLocalControlledLeaderSuppressesMemberIdleBroadcastUntilSessionEnds() {
        TaskMaintenanceService service = service();
        String session = "cr212-local";
        service.registerLocalTeamSessionCandidate(
                session,
                List.of("leader-window", "member-window"),
                "cr212-test",
                null,
                Map.of("leader-window", "10001", "member-window", "20002"));

        TaskExecutionContext member = memberContext(session, "member-window");
        service.recordLocalTeamTooltipGroup(
                member, "member-window", "20002", "hash-local", "10001", "MEMBER", "cr212-test");

        require(service.shouldSuppressIdleMaintenanceBroadcast(member),
                "local-controlled leader should suppress member idle unexpected broadcast scan");
        require(service.shouldSuppressIdleMaintenanceBroadcast(member),
                "pause/resume must not change suppression because no session lifecycle event occurred");

        service.completeLocalTeamSessionWindow(session, "leader-window", "cr212-test:leader-stop");
        require(!service.shouldSuppressIdleMaintenanceBroadcast(member),
                "leader stop/session completion must restore old idle broadcast scanning");
    }

    public void testExternalLeaderAndLeaderAbsentDoNotSuppressIdleBroadcast() {
        TaskMaintenanceService externalService = service();
        String externalSession = "cr212-external";
        externalService.registerLocalTeamSessionCandidate(
                externalSession,
                List.of("member-a", "member-b"),
                "cr212-test",
                null,
                Map.of("member-a", "20002", "member-b", "30003"));
        TaskExecutionContext externalMember = memberContext(externalSession, "member-a");
        externalService.recordLocalTeamTooltipGroup(
                externalMember, "member-a", "20002", "hash-external", "99999", "MEMBER", "cr212-test");
        require(!externalService.shouldSuppressIdleMaintenanceBroadcast(externalMember),
                "external leader must not suppress member idle broadcast scanning");
        require(!externalService.isPendingLocalSupportLeaderDetection(externalMember),
                "external leader evidence should restore old scanning instead of staying pending");

        TaskMaintenanceService absentService = service();
        String absentSession = "cr212-absent";
        absentService.registerLocalTeamSessionCandidate(
                absentSession,
                List.of("member-a", "member-b"),
                "cr212-test",
                null,
                Map.of("member-a", "20002", "member-b", "30003"));
        TaskExecutionContext absentA = memberContext(absentSession, "member-a");
        TaskExecutionContext absentB = memberContext(absentSession, "member-b");
        absentService.markLocalTeamWindowRoleDetected(absentA, "member-a", "MEMBER", "cr212-test");
        absentService.markLocalTeamWindowRoleDetected(absentB, "member-b", "MEMBER", "cr212-test");
        require(!absentService.shouldSuppressIdleMaintenanceBroadcast(absentA),
                "leader-absent session must keep old idle broadcast scanning");
        require(!absentService.isPendingLocalSupportLeaderDetection(absentA),
                "leader-absent session must not keep members deferred forever");
    }

    public void testMixedHashGroupsSuppressOnlyLocalLeaderGroupWhenLocalGroupWritesFirst() {
        TaskMaintenanceService service = service();
        String session = "cr212-mixed-local-first";
        registerMixedSession(service, session);

        TaskExecutionContext localMember = memberContext(session, "local-member");
        TaskExecutionContext externalMember = memberContext(session, "external-member");
        service.recordLocalTeamTooltipGroup(
                localMember, "local-member", "20002", "hash-local", "10001", "MEMBER", "cr212-test:local-first");
        service.recordLocalTeamTooltipGroup(
                externalMember, "external-member", "30003", "hash-external", "99999", "MEMBER", "cr212-test:external-second");

        require(service.isLocalSupportMemberSession(localMember),
                "local leader group member must remain a local support member");
        require(!service.isLocalSupportMemberSession(externalMember),
                "external leader group member must not inherit unrelated local support membership");
        require(service.shouldSuppressIdleMaintenanceBroadcast(localMember),
                "local leader group member must suppress even after an external group writes later");
        require(!service.shouldSuppressIdleMaintenanceBroadcast(externalMember),
                "external leader group member must not inherit session-level suppression");
    }

    public void testMixedHashGroupsSuppressOnlyLocalLeaderGroupWhenExternalGroupWritesFirst() {
        TaskMaintenanceService service = service();
        String session = "cr212-mixed-external-first";
        registerMixedSession(service, session);

        TaskExecutionContext localMember = memberContext(session, "local-member");
        TaskExecutionContext externalMember = memberContext(session, "external-member");
        service.recordLocalTeamTooltipGroup(
                externalMember, "external-member", "30003", "hash-external", "99999", "MEMBER", "cr212-test:external-first");
        service.recordLocalTeamTooltipGroup(
                localMember, "local-member", "20002", "hash-local", "10001", "MEMBER", "cr212-test:local-second");

        require(service.isLocalSupportMemberSession(localMember),
                "local leader group member must be local support when its group writes after an external group");
        require(!service.isLocalSupportMemberSession(externalMember),
                "external leader group member must stay outside local support after local group writes later");
        require(service.shouldSuppressIdleMaintenanceBroadcast(localMember),
                "local leader group member must suppress when its group writes after an external group");
        require(!service.shouldSuppressIdleMaintenanceBroadcast(externalMember),
                "external leader group member must stay unsuppressed after local group writes later");
    }

    private static TaskMaintenanceService service() {
        return new TaskMaintenanceService(
                null,
                new GameContext(),
                null,
                null,
                new WindowTaskContextHolder(new WindowIsolationProperties()),
                null,
                null,
                null);
    }

    private static void registerMixedSession(TaskMaintenanceService service, String session) {
        service.registerLocalTeamSessionCandidate(
                session,
                List.of("local-leader", "local-member", "external-member"),
                "cr212-test:mixed-candidates",
                null,
                Map.of(
                        "local-leader", "10001",
                        "local-member", "20002",
                        "external-member", "30003"));
    }

    private static TaskExecutionContext memberContext(String session, String windowId) {
        return TaskExecutionContext.builder()
                .taskCode("auto_battle")
                .requestedTaskCode("wubei")
                .windowId(windowId)
                .windowRole("MEMBER")
                .localTeamSessionKey(session)
                .localLeaderPresent(true)
                .localSupportMember(true)
                .taskRunId(212L)
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
}
