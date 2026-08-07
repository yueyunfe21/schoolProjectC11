package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.LocalServiceStepDispatcher;
import com.bot.dhxy.cloud.turn.local.BagLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.GiveItemLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.LeftTopStatusLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.MapSurveyPointerLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.MetricsLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.QuestLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.TaskTrackerLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.UiLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.WholeTaskRuntimeLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.XinshouCombatLocalMechanics;
import com.bot.dhxy.cloud.turn.local.XinshouDragLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.XinshouTitleMechanicalExecutor;
import com.bot.dhxy.cloud.turn.local.XinshouTrackerLinkChainLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.host.HostLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.bot.dhxy.cloud.turn.protocol.TurnXinshouMechanicalAction;
import com.bot.dhxy.cloud.turn.protocol.TurnXinshouMechanicalArguments;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.awt.Point;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XinshouRecoveryMechanicalDispatcherContractTest {

    private static final Unsafe UNSAFE = findUnsafe();

    @Test
    void pressEscapeRunsOnceAndFailureDoesNotRetry() {
        RecordingInput successInput = new RecordingInput(true);
        LocalServiceExecution success = dispatcher(mechanics(successInput, true)).execute(
                call(TurnXinshouMechanicalAction.PRESS_ESCAPE, null),
                0, null, null, () -> true, "action-1", "device-1", "window-1", null);
        assertEquals(TurnStepResult.Status.COMPLETED, success.status());
        assertEquals(1, successInput.escapeCalls);

        RecordingInput failedInput = new RecordingInput(false);
        LocalServiceExecution failed = dispatcher(mechanics(failedInput, true)).execute(
                call(TurnXinshouMechanicalAction.PRESS_ESCAPE, null),
                0, null, null, () -> true, "action-2", "device-1", "window-1", null);
        assertEquals(TurnStepResult.Status.FAILED, failed.status());
        assertEquals(1, failedInput.escapeCalls);
    }

    @Test
    void eachAllowListedTemplateMatchesAndClicksExactlyOnce() {
        for (String template : new String[]{"tiaoguo.png", "quedingguan_.png", "confirm.png"}) {
            RecordingInput input = new RecordingInput(true);
            int[] resolutions = {0};
            XinshouRecoveryLocalMechanics mechanics = new XinshouRecoveryLocalMechanics(
                    ignored -> {
                        resolutions[0]++;
                        return XinshouRecoveryLocalMechanics.TargetResolution.matched(
                                new Point(500, 400));
                    },
                    input);

            LocalServiceExecution result = dispatcher(mechanics).execute(
                    call(TurnXinshouMechanicalAction.CLICK_RECOVERY_TEMPLATE, template),
                    0, null, null, () -> true, "action-3", "device-1", "window-1", null);

            assertEquals(TurnStepResult.Status.COMPLETED, result.status(), template);
            assertEquals(1, resolutions[0], template);
            assertEquals(1, input.clickCalls, template);
            assertEquals(template, input.lastTemplate, template);
        }
    }

    @Test
    void unsupportedOrUnmatchedTemplateFailsClosedWithoutInputOrRetry() {
        RecordingInput unsupportedInput = new RecordingInput(true);
        int[] unsupportedResolutions = {0};
        XinshouRecoveryLocalMechanics unsupportedMechanics = new XinshouRecoveryLocalMechanics(
                ignored -> {
                    unsupportedResolutions[0]++;
                    return XinshouRecoveryLocalMechanics.TargetResolution.matched(
                            new Point(500, 400));
                },
                unsupportedInput);
        LocalServiceExecution unsupported = dispatcher(unsupportedMechanics).execute(
                call(TurnXinshouMechanicalAction.CLICK_RECOVERY_TEMPLATE, "fake.png"),
                0, null, null, () -> true, "action-4", "device-1", "window-1", null);
        assertEquals(TurnStepResult.Status.FAILED, unsupported.status());
        assertEquals("INVALID_XINSHOU_MECHANICAL_CALL", unsupported.code());
        assertEquals(0, unsupportedResolutions[0]);
        assertEquals(0, unsupportedInput.clickCalls);

        RecordingInput missInput = new RecordingInput(true);
        int[] missResolutions = {0};
        XinshouRecoveryLocalMechanics missMechanics = new XinshouRecoveryLocalMechanics(
                ignored -> {
                    missResolutions[0]++;
                    return XinshouRecoveryLocalMechanics.TargetResolution.of(
                            XinshouRecoveryLocalMechanics.ResolutionStatus.TEMPLATE_NOT_MATCHED);
                },
                missInput);
        LocalServiceExecution miss = dispatcher(missMechanics).execute(
                call(TurnXinshouMechanicalAction.CLICK_RECOVERY_TEMPLATE, "confirm.png"),
                0, null, null, () -> true, "action-5", "device-1", "window-1", null);
        assertEquals(TurnStepResult.Status.FAILED, miss.status());
        assertTrue(miss.code().endsWith("TEMPLATE_NOT_MATCHED"));
        assertEquals(1, missResolutions[0]);
        assertEquals(0, missInput.clickCalls);
    }

    private static XinshouRecoveryLocalMechanics mechanics(
            RecordingInput input,
            boolean matched) {
        return new XinshouRecoveryLocalMechanics(
                ignored -> matched
                        ? XinshouRecoveryLocalMechanics.TargetResolution.matched(
                                new Point(500, 400))
                        : XinshouRecoveryLocalMechanics.TargetResolution.of(
                                XinshouRecoveryLocalMechanics.ResolutionStatus.TEMPLATE_NOT_MATCHED),
                input);
    }

    private static TurnLocalServiceCall call(
            TurnXinshouMechanicalAction action,
            String template) {
        return new TurnLocalServiceCall(
                TurnLocalOperation.XINSHOU_MECHANICAL_ACTION,
                new TurnXinshouMechanicalArguments(action, template));
    }

    private static LocalServiceStepDispatcher dispatcher(
            XinshouRecoveryLocalMechanics recoveryMechanics) {
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
                allocate(XinshouTitleMechanicalExecutor.class),
                allocate(XinshouCombatLocalMechanics.class),
                recoveryMechanics,
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

    private static final class RecordingInput
            implements XinshouRecoveryLocalMechanics.RecoveryInput {
        private final boolean result;
        private int escapeCalls;
        private int clickCalls;
        private String lastTemplate;

        private RecordingInput(boolean result) {
            this.result = result;
        }

        @Override
        public boolean pressEscape() {
            escapeCalls++;
            return result;
        }

        @Override
        public boolean clickTemplate(String templateName, Point absolutePoint) {
            clickCalls++;
            lastTemplate = templateName;
            return result;
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
