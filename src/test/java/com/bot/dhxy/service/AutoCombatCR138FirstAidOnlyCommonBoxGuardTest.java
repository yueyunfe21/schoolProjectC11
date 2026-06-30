package com.bot.dhxy.service;

import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.task.transaction.TaskTurnCoordinator;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;

import java.lang.reflect.Method;

/**
 * Behavior guard for CR138 FIRST_AID_ONLY common-box isolation.
 */
public final class AutoCombatCR138FirstAidOnlyCommonBoxGuardTest {

    private AutoCombatCR138FirstAidOnlyCommonBoxGuardTest() {
    }

    public static void main(String[] args) throws Exception {
        verifyFirstAidOnlyDoesNotConsumeCommonBox();
        verifyPathingWindowCanConsumeCommonBox();
        System.out.println("AutoCombatCR138FirstAidOnlyCommonBoxGuardTest passed");
    }

    private static void verifyFirstAidOnlyDoesNotConsumeCommonBox() throws Exception {
        Fixture fixture = new Fixture();
        fixture.commonBox.hasPending = true;
        fixture.commonBox.consumeResult = true;

        fixture.maintenance.openTeamFirstAidMaintenanceWindow(
                fixture.leader, "xiuluo_v2", 7, "cr138-test:first-aid-only");
        boolean consumed = fixture.runPendingMemberBox();

        require(!consumed, "FIRST_AID_ONLY local window must not consume common box");
        require(fixture.commonBox.consumeCalls == 0,
                "FIRST_AID_ONLY local window must not call common-box consume");
    }

    private static void verifyPathingWindowCanConsumeCommonBox() throws Exception {
        Fixture fixture = new Fixture();
        fixture.commonBox.hasPending = true;
        fixture.commonBox.consumeResult = true;

        fixture.maintenance.openTeamPathingMaintenanceWindow(
                fixture.leader, "xiuluo_v2", 8, "cr138-test:pathing");
        boolean consumed = fixture.runPendingMemberBox();

        require(consumed, "PATHING_WINDOW local window should permit common-box consumption");
        require(fixture.commonBox.consumeCalls == 1,
                "PATHING_WINDOW local window must call common-box consume once");
    }

    private static final class Fixture {
        private final GameContext gameContext = new GameContext();
        private final WindowTaskContextHolder holder = new WindowTaskContextHolder(new WindowIsolationProperties());
        private final FakeCommonBoxService commonBox = new FakeCommonBoxService();
        private final TaskMaintenanceService maintenance;
        private final AutoCombatService autoCombat;
        private final TaskExecutionContext leader;
        private final TaskExecutionContext member;

        private Fixture() {
            WindowRuntimeContext memberWindow = new WindowRuntimeContext("member-window", gameContext);
            memberWindow.setRole(WindowRole.MEMBER);
            holder.bind(memberWindow);
            TaskTurnCoordinator coordinator = new TaskTurnCoordinator(holder);
            maintenance = new TaskMaintenanceService(null, new GameContext(), null, null, holder);
            autoCombat = new AutoCombatService(
                    gameContext,
                    null,
                    null,
                    null,
                    null,
                    maintenance,
                    null,
                    commonBox,
                    null,
                    holder,
                    coordinator);
            leader = TaskExecutionContext.builder()
                    .taskCode("xiuluo_v2")
                    .requestedTaskCode("xiuluo_v2")
                    .windowId("leader-window")
                    .windowRole("LEADER")
                    .localTeamSessionKey("local-session-1")
                    .localLeaderWindowId("leader-window")
                    .localLeaderPresent(true)
                    .taskRunId(138L)
                    .build();
            member = TaskExecutionContext.builder()
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
            maintenance.markLocalTeamLeaderDetected(leader, "leader-window", "cr138-test:live-leader");
        }

        private boolean runPendingMemberBox() throws Exception {
            Method method = AutoCombatService.class.getDeclaredMethod(
                    "runPendingMemberCommonBoxIfAllowed", TaskExecutionContext.class, String.class);
            method.setAccessible(true);
            return (boolean) method.invoke(autoCombat, member, "cr138-test");
        }
    }

    private static final class FakeCommonBoxService extends CommonBoxService {
        private boolean hasPending;
        private boolean consumeResult;
        private int consumeCalls;

        private FakeCommonBoxService() {
            super(null, null, null, null);
        }

        @Override
        public boolean hasPendingBoxForCurrentWindow(TaskExecutionContext context, String sourceTask) {
            return hasPending;
        }

        @Override
        public boolean consumePendingBoxIfAllowed(TaskExecutionContext context, String sourceTask, String source) {
            consumeCalls++;
            return consumeResult;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
