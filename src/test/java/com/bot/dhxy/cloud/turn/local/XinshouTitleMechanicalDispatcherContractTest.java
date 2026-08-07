package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.LocalServiceStepDispatcher;
import com.bot.dhxy.cloud.turn.local.host.HostLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.bot.dhxy.cloud.turn.protocol.TurnXinshouMechanicalAction;
import com.bot.dhxy.cloud.turn.protocol.TurnXinshouMechanicalArguments;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.window.observation.XinshouRecoveryLocalMechanics;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.awt.Point;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XinshouTitleMechanicalDispatcherContractTest {

    private static final Unsafe UNSAFE = findUnsafe();

    @Test
    void dispatcherRunsEachTitleActionExactlyOnceThroughTheProductionExecutor() {
        assertAction(TurnXinshouMechanicalAction.CONFIRM_ADOPTION,
                new ExpectedCounts(1, 0, 0, 1, 0, 0, 0, 0));
        assertAction(TurnXinshouMechanicalAction.USE_UPGRADE_ITEM_AND_CLOSE_GENERIC_WINDOWS,
                new ExpectedCounts(0, 1, 1, 0, 0, 0, 0, 0));
        assertAction(TurnXinshouMechanicalAction.USE_SHELL_AND_BLOW,
                new ExpectedCounts(0, 1, 0, 1, 0, 1, 0, 0));
        assertAction(TurnXinshouMechanicalAction.HAND_IN_MATERIALS,
                new ExpectedCounts(0, 0, 0, 1, 0, 1, 1, 0));
        assertAction(TurnXinshouMechanicalAction.REPAIR_ITEMS_ONCE,
                new ExpectedCounts(0, 1, 0, 0, 5, 6, 0, 0));
        assertAction(TurnXinshouMechanicalAction.CLOSE_REPAIR_WINDOW,
                new ExpectedCounts(0, 0, 0, 0, 0, 1, 0, 0));
        assertAction(TurnXinshouMechanicalAction.USE_LUNHUI_ITEM_AND_START,
                new ExpectedCounts(0, 1, 0, 1, 0, 2, 0, 1));
    }

    @Test
    void failedMechanicalActionIsReturnedWithoutLocalRetry() {
        RecordingPort port = new RecordingPort();
        port.useItemResult = false;
        LocalServiceExecution result = dispatcher(new XinshouTitleMechanicalExecutor(port)).execute(
                call(TurnXinshouMechanicalAction.USE_SHELL_AND_BLOW),
                0, null, null, () -> true, "action-1", "device-1", "window-1", null);

        assertEquals(TurnStepResult.Status.FAILED, result.status());
        assertEquals(1, port.useItemCalls);
        assertEquals(0, port.regionMatchCalls);
        assertEquals(0, port.moveClickCalls);
    }

    private static void assertAction(
            TurnXinshouMechanicalAction action,
            ExpectedCounts expected) {
        RecordingPort port = new RecordingPort();
        LocalServiceExecution result = dispatcher(new XinshouTitleMechanicalExecutor(port)).execute(
                call(action),
                0, null, null, () -> true, "action-1", "device-1", "window-1", null);

        assertEquals(TurnStepResult.Status.COMPLETED, result.status(), action.name());
        assertTrue(result.code().startsWith("XINSHOU_MECHANICAL_" + action), action.name());
        assertEquals(expected.submitCalls(), port.submitCalls, action.name());
        assertEquals(expected.useItemCalls(), port.useItemCalls, action.name());
        assertEquals(expected.closeCalls(), port.closeCalls, action.name());
        assertEquals(expected.regionMatchCalls(), port.regionMatchCalls, action.name());
        assertEquals(expected.absoluteMatchCalls(), port.absoluteMatchCalls, action.name());
        assertEquals(expected.moveClickCalls(), port.moveClickCalls, action.name());
        assertEquals(expected.giveCalls(), port.giveCalls, action.name());
        assertEquals(expected.sleepCalls(), port.sleepCalls, action.name());
    }

    private static TurnLocalServiceCall call(TurnXinshouMechanicalAction action) {
        TurnXinshouMechanicalArguments arguments =
                new TurnXinshouMechanicalArguments(action, null);
        return new TurnLocalServiceCall(TurnLocalOperation.XINSHOU_MECHANICAL_ACTION, arguments);
    }

    private static LocalServiceStepDispatcher dispatcher(XinshouTitleMechanicalExecutor titleExecutor) {
        return new LocalServiceStepDispatcher(
                allocate(BagLocalOperationExecutor.class),
                allocate(UiLocalOperationExecutor.class),
                allocate(GiveItemLocalOperationExecutor.class),
                allocate(QuestLocalOperationExecutor.class),
                allocate(TaskTrackerLocalOperationExecutor.class),
                allocate(WholeTaskRuntimeLocalOperationExecutor.class),
                allocate(MetricsLocalOperationExecutor.class),
                allocate(HostLocalOperationExecutor.class),
                allocate(MapSurveyPointerLocalOperationExecutor.class),
                allocate(LeftTopStatusLocalOperationExecutor.class),
                allocate(XinshouDragLocalOperationExecutor.class),
                allocate(XinshouTrackerLinkChainLocalOperationExecutor.class),
                titleExecutor,
                allocate(XinshouCombatLocalMechanics.class),
                allocate(XinshouRecoveryLocalMechanics.class),
                noOpInputProvider(),
                allocate(InputSequences.class),
                allocate(WindowTaskContextHolder.class));
    }

    private static InputProvider noOpInputProvider() {
        return (InputProvider) Proxy.newProxyInstance(
                InputProvider.class.getClassLoader(),
                new Class<?>[]{InputProvider.class},
                (proxy, method, args) -> null);
    }

    private record ExpectedCounts(
            int submitCalls,
            int useItemCalls,
            int closeCalls,
            int regionMatchCalls,
            int absoluteMatchCalls,
            int moveClickCalls,
            int giveCalls,
            int sleepCalls) {
    }

    private static final class RecordingPort
            implements XinshouTitleMechanicalExecutor.MechanicalPort {
        private int submitCalls;
        private int useItemCalls;
        private int closeCalls;
        private int regionMatchCalls;
        private int absoluteMatchCalls;
        private int moveClickCalls;
        private int giveCalls;
        private int sleepCalls;
        private boolean useItemResult = true;

        @Override
        public boolean submitInput(String description, List<InputAction> actions) {
            submitCalls++;
            return true;
        }

        @Override
        public boolean useFirstTabItem(String source, String template) {
            useItemCalls++;
            return useItemResult;
        }

        @Override
        public void closeAllGenericWindows() {
            closeCalls++;
        }

        @Override
        public boolean clickGiveButtonAfterLocalSelection() {
            giveCalls++;
            return true;
        }

        @Override
        public int[] scaledRect(int relativeX, int relativeY, int width, int height) {
            return new int[]{relativeX, relativeY, width, height};
        }

        @Override
        public Point findImageInRegion(String template, int[] roi, double matchRate) {
            regionMatchCalls++;
            return new Point(500, 400);
        }

        @Override
        public Point findImageAbsoluteCoordinate(String template, double matchRate) {
            absoluteMatchCalls++;
            return new Point(500, 400);
        }

        @Override
        public boolean moveAndClickLeft(
                String description, int screenX, int screenY, int settleMs, int delayMs) {
            moveClickCalls++;
            return true;
        }

        @Override
        public boolean sleep(long millis) {
            sleepCalls++;
            return true;
        }
    }

    private static <T> T allocate(Class<T> type) {
        try {
            return type.cast(UNSAFE.allocateInstance(type));
        } catch (InstantiationException error) {
            throw new AssertionError("cannot allocate inert test dependency " + type.getName(), error);
        }
    }

    private static Unsafe findUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }
}
