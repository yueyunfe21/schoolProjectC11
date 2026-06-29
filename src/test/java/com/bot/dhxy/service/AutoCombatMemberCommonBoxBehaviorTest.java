package com.bot.dhxy.service;

import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.task.transaction.TaskTurnCoordinator;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Behavioral guard for CR120 member common-box ordering.
 */
public final class AutoCombatMemberCommonBoxBehaviorTest {

    private AutoCombatMemberCommonBoxBehaviorTest() {
    }

    public static void main(String[] args) throws Exception {
        verifyMemberBoxConsumesWithoutFirstAidPending();
        verifyMemberBoxDoesNotClearPendingFirstAid();
        System.out.println("AutoCombatMemberCommonBoxBehaviorTest passed");
    }

    private static void verifyMemberBoxConsumesWithoutFirstAidPending() throws Exception {
        Fixture fixture = new Fixture();
        fixture.commonBox.hasPending = true;
        fixture.commonBox.consumeResult = true;

        boolean consumed = fixture.runPendingMemberBox();

        require(consumed, "Member common-box must consume even when follower first-aid is not pending");
        require(fixture.commonBox.hasPendingCalls == 1, "Member common-box hook must check pending box state");
        require(fixture.commonBox.consumeCalls == 1, "Member common-box hook must consume the pending box");
    }

    private static void verifyMemberBoxDoesNotClearPendingFirstAid() throws Exception {
        Fixture fixture = new Fixture();
        fixture.commonBox.hasPending = true;
        fixture.commonBox.consumeResult = true;
        fixture.setPendingFirstAid(true);

        boolean consumed = fixture.runPendingMemberBox();

        require(consumed, "Member common-box should consume before first-aid when both are pending");
        require(fixture.isPendingFirstAid(), "Member common-box consume must leave follower first-aid pending");
    }

    private static final class Fixture {
        private final GameContext gameContext = new GameContext();
        private final WindowTaskContextHolder holder = new WindowTaskContextHolder(new WindowIsolationProperties());
        private final FakeCommonBoxService commonBox = new FakeCommonBoxService();
        private final AutoCombatService service;
        private final TaskExecutionContext context;

        private Fixture() {
            WindowRuntimeContext window = new WindowRuntimeContext("member-window", gameContext);
            window.setRole(WindowRole.MEMBER);
            holder.bind(window);
            TaskTurnCoordinator coordinator = new TaskTurnCoordinator(holder);
            service = new AutoCombatService(
                    gameContext,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    commonBox,
                    null,
                    holder,
                    coordinator);
            context = TaskExecutionContext.builder()
                    .taskCode("auto_battle")
                    .requestedTaskCode("xiuluo_v2")
                    .windowId("member-window")
                    .windowRole("MEMBER")
                    .taskRunId(120L)
                    .build();
        }

        private boolean runPendingMemberBox() throws Exception {
            Method method = AutoCombatService.class.getDeclaredMethod(
                    "runPendingMemberCommonBoxIfAllowed", TaskExecutionContext.class, String.class);
            method.setAccessible(true);
            return (boolean) method.invoke(service, context, "cr120-test");
        }

        private void setPendingFirstAid(boolean pending) throws Exception {
            Object state = state();
            Field field = state.getClass().getDeclaredField("pendingFollowerFirstAid");
            field.setAccessible(true);
            field.setBoolean(state, pending);
        }

        private boolean isPendingFirstAid() throws Exception {
            Object state = state();
            Field field = state.getClass().getDeclaredField("pendingFollowerFirstAid");
            field.setAccessible(true);
            return field.getBoolean(state);
        }

        private Object state() throws Exception {
            Method method = AutoCombatService.class.getDeclaredMethod("state");
            method.setAccessible(true);
            return method.invoke(service);
        }
    }

    private static final class FakeCommonBoxService extends CommonBoxService {
        private boolean hasPending;
        private boolean consumeResult;
        private int hasPendingCalls;
        private int consumeCalls;

        private FakeCommonBoxService() {
            super(null, null, null, null);
        }

        @Override
        public boolean hasPendingBoxForCurrentWindow(TaskExecutionContext context, String sourceTask) {
            hasPendingCalls++;
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
