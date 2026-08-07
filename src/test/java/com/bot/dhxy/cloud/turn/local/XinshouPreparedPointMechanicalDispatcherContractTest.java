package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.LocalServiceStepDispatcher;
import com.bot.dhxy.cloud.turn.local.host.HostLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.bot.dhxy.cloud.turn.protocol.TurnXinshouMechanicalAction;
import com.bot.dhxy.cloud.turn.protocol.TurnXinshouMechanicalArguments;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.observation.XinshouRecoveryLocalMechanics;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XinshouPreparedPointMechanicalDispatcherContractTest {

    private static final Unsafe UNSAFE = findUnsafe();

    @Test
    void preparedPointTranslatesAcrossWindowMovementAndClicksExactlyOnce() {
        try (Fixture fixture = new Fixture()) {
            fixture.bindAt(300, 400, 1024, 768);

            LocalServiceExecution result = fixture.execute(arguments(
                    321, 654,
                    100, 200, 1024, 768));

            assertEquals(TurnStepResult.Status.COMPLETED, result.status());
            assertEquals("XINSHOU_MECHANICAL_CLICK_PREPARED_POINT_COMPLETED", result.code());
            assertEquals(1, fixture.input.moveCalls.get());
            assertEquals(1, fixture.input.clickCalls.get());
            assertEquals(521, fixture.input.moveX);
            assertEquals(854, fixture.input.moveY);
            assertEquals(521, fixture.input.clickX);
            assertEquals(854, fixture.input.clickY);
            assertEquals(250, fixture.input.clickDelayMs);
        }
    }

    @Test
    void negativeDesktopCoordinatesRemainValidWhenTheWindowOnlyMoved() {
        try (Fixture fixture = new Fixture()) {
            fixture.bindAt(-900, 200, 1024, 768);

            LocalServiceExecution result = fixture.execute(arguments(
                    -1000, 300,
                    -1200, 100, 1024, 768));

            assertEquals(TurnStepResult.Status.COMPLETED, result.status());
            assertEquals(1, fixture.input.clickCalls.get());
            assertEquals(-700, fixture.input.clickX);
            assertEquals(400, fixture.input.clickY);
        }
    }

    @Test
    void bindingGenerationDriftAfterEnqueueRejectsWithoutAnyClick() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.bindAt(301, 400, 1024, 768);
            fixture.input.blockWorker();
            CompletableFuture<LocalServiceExecution> pending = CompletableFuture.supplyAsync(
                    () -> fixture.execute(arguments(
                            321, 654,
                            100, 200, 1024, 768)));
            fixture.input.awaitQueuedRequest();

            fixture.bindAt(300, 400, 1024, 768);
            fixture.input.releaseWorker();
            LocalServiceExecution result = pending.get(3, TimeUnit.SECONDS);

            assertEquals(TurnStepResult.Status.FAILED, result.status());
            assertEquals("XINSHOU_MECHANICAL_CLICK_PREPARED_POINT_STALE", result.code());
            assertEquals(0, fixture.input.moveCalls.get());
            assertEquals(0, fixture.input.clickCalls.get());
        }
    }

    @Test
    void missingWindowChangedSizeAndOutOfSourcePointAllFailWithoutInput() {
        try (Fixture missing = new Fixture();
             Fixture resized = new Fixture();
             Fixture outside = new Fixture()) {
            missing.context.setNativeBinding(WindowNativeBinding.empty());
            assertFailedWithoutInput(
                    missing,
                    arguments(321, 654, 0, 0, 1024, 768),
                    "XINSHOU_MECHANICAL_CLICK_PREPARED_POINT_WINDOW_UNAVAILABLE");

            resized.bindAt(300, 400, 1000, 768);
            assertFailedWithoutInput(
                    resized,
                    arguments(321, 654, 100, 200, 1024, 768),
                    "XINSHOU_MECHANICAL_CLICK_PREPARED_POINT_WINDOW_SIZE_CHANGED");

            outside.bindAt(0, 0, 1024, 768);
            assertFailedWithoutInput(
                    outside,
                    arguments(1024, 100, 0, 0, 1024, 768),
                    "INVALID_XINSHOU_MECHANICAL_CALL");
        }
    }

    private static void assertFailedWithoutInput(
            Fixture fixture,
            TurnXinshouMechanicalArguments arguments,
            String expectedCode) {
        LocalServiceExecution result = fixture.execute(arguments);
        assertEquals(TurnStepResult.Status.FAILED, result.status());
        assertEquals(expectedCode, result.code());
        assertEquals(0, fixture.input.moveCalls.get());
        assertEquals(0, fixture.input.clickCalls.get());
    }

    private static TurnXinshouMechanicalArguments arguments(
            int screenX,
            int screenY,
            int sourceLeft,
            int sourceTop,
            int sourceWidth,
            int sourceHeight) {
        return new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CLICK_PREPARED_POINT,
                null,
                screenX,
                screenY,
                sourceLeft,
                sourceTop,
                sourceWidth,
                sourceHeight);
    }

    private static final class Fixture implements AutoCloseable {
        private final FrozenExactInputHarness input = new FrozenExactInputHarness();
        private final WindowTaskContextHolder holder = input.contextHolder;
        private final WindowRuntimeContext context = input.newContext(
                "window-1",
                new WindowNativeBinding(
                        "12345", "game", "class", 77L, 0, 0, 1024, 768));
        private final LocalServiceStepDispatcher dispatcher = new LocalServiceStepDispatcher(
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
                allocate(XinshouTitleMechanicalExecutor.class),
                allocate(XinshouCombatLocalMechanics.class),
                allocate(XinshouRecoveryLocalMechanics.class),
                input.inputProvider,
                input.inputSequences,
                holder);

        private void bindAt(int left, int top, int width, int height) {
            context.setNativeBinding(new WindowNativeBinding(
                    "12345", "game", "class", 77L, left, top, width, height));
        }

        private LocalServiceExecution execute(TurnXinshouMechanicalArguments arguments) {
            TurnLocalServiceCall call = new TurnLocalServiceCall(
                    TurnLocalOperation.XINSHOU_MECHANICAL_ACTION,
                    arguments);
            return holder.callWith(context, () -> dispatcher.execute(
                    call,
                    0,
                    null,
                    null,
                    () -> true,
                    "action-1",
                    "device-1",
                    "window-1",
                    null));
        }

        @Override
        public void close() {
            input.close();
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
