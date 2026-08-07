package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.LocalServiceStepDispatcher;
import com.bot.dhxy.cloud.turn.local.host.HostLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnAction;
import com.bot.dhxy.cloud.turn.protocol.TurnProtocolValidator;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.bot.dhxy.cloud.turn.protocol.TurnStepType;
import com.bot.dhxy.cloud.turn.protocol.TurnXinshouMechanicalAction;
import com.bot.dhxy.cloud.turn.protocol.TurnXinshouMechanicalArguments;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.window.observation.XinshouRecoveryLocalMechanics;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XinshouCombatMechanicalDispatcherContractTest {

    private static final Unsafe UNSAFE = findUnsafe();

    @Test
    void ordinaryCommandDispatchesTwoFocusedAltAInputsAndReturnsTypedSuccess() {
        RecordingPort port = new RecordingPort();

        LocalServiceExecution result = dispatcher(port).execute(
                call(new TurnXinshouMechanicalArguments(
                        TurnXinshouMechanicalAction.PRESS_ORDINARY_AUTO_COMBAT,
                        null)),
                0, null, null, () -> true, "action-1", "device-1", "window-1", null);

        assertEquals(TurnStepResult.Status.COMPLETED, result.status());
        assertEquals(
                "XINSHOU_MECHANICAL_PRESS_ORDINARY_AUTO_COMBAT_COMPLETED",
                result.code());
        assertEquals(List.of("FOCUSED", "ALT_A", "ALT_A"), port.events);
        assertEquals(1, port.executionCount);
    }

    @Test
    void cloudWireCommandCrossesProtocolValidatorAndProductionDispatcher() throws IOException {
        Path commandArtifact = connectivityDirectory().resolve("xinshou-combat-command.json");
        assertTrue(Files.isRegularFile(commandArtifact),
                () -> "missing Cloud connectivity artifact: " + commandArtifact);

        TurnAction action = TurnProtocolValidator.requireValid(
                new ObjectMapper().readValue(commandArtifact.toFile(), TurnAction.class));
        assertEquals("device", action.deviceId());
        assertEquals("window", action.windowId());
        assertEquals(1, action.steps().size());
        assertEquals(TurnStepType.LOCAL_SERVICE, action.steps().get(0).type());

        RecordingPort port = new RecordingPort();
        LocalServiceExecution result = dispatcher(port).execute(
                action.steps().get(0).localService(),
                action.steps().get(0).index(),
                null,
                null,
                () -> true,
                action.actionId(),
                action.deviceId(),
                action.windowId(),
                null);

        assertEquals(TurnStepResult.Status.COMPLETED, result.status());
        assertEquals(
                "XINSHOU_MECHANICAL_PRESS_ORDINARY_AUTO_COMBAT_COMPLETED",
                result.code());
        assertEquals(List.of("FOCUSED", "ALT_A", "ALT_A"), port.events);
        assertEquals(1, port.executionCount);
    }

    @Test
    void captureCommandDispatchesTheValidatedPointOnceAndReturnsTypedSuccess() {
        RecordingPort port = new RecordingPort();
        port.panelVisibility = XinshouCombatLocalMechanics.PanelVisibility.ABSENT;

        LocalServiceExecution result = dispatcher(port).execute(
                call(captureArguments()),
                0, null, null, () -> true, "action-1", "device-1", "window-1", null);

        assertEquals(TurnStepResult.Status.COMPLETED, result.status());
        assertEquals("XINSHOU_MECHANICAL_CAPTURE_COMBAT_COMPLETED", result.code());
        assertEquals(
                List.of(
                        "FOCUSED",
                        "CONTAINS:521,854",
                        "ALT_B",
                        "WAIT:1000",
                        "PROBE",
                        "CLICK:521,854"),
                port.events);
        assertEquals(1, port.executionCount);
    }

    @Test
    void restoreCommandDispatchesOneFocusedSequenceAndReturnsTypedSuccess() {
        RecordingPort port = new RecordingPort();
        port.panelVisibility = XinshouCombatLocalMechanics.PanelVisibility.VISIBLE;

        LocalServiceExecution result = dispatcher(port).execute(
                call(new TurnXinshouMechanicalArguments(
                        TurnXinshouMechanicalAction.RESTORE_AUTO_COMBAT,
                        null)),
                0, null, null, () -> true, "action-1", "device-1", "window-1", null);

        assertEquals(TurnStepResult.Status.COMPLETED, result.status());
        assertEquals("XINSHOU_MECHANICAL_RESTORE_AUTO_COMBAT_COMPLETED", result.code());
        assertEquals(
                List.of(
                        "FOCUSED",
                        "ALT_A",
                        "WAIT:1000",
                        "ALT_A",
                        "WAIT:1000",
                        "PROBE"),
                port.events);
        assertEquals(1, port.executionCount);
    }

    @Test
    void mechanicalFailureRemainsTypedAndIsNotRetriedLocally() {
        RecordingPort port = new RecordingPort();
        port.panelVisibility = XinshouCombatLocalMechanics.PanelVisibility.VISIBLE;

        LocalServiceExecution result = dispatcher(port).execute(
                call(captureArguments()),
                0, null, null, () -> true, "action-1", "device-1", "window-1", null);

        assertEquals(TurnStepResult.Status.FAILED, result.status());
        assertEquals(
                "XINSHOU_MECHANICAL_CAPTURE_COMBAT_PANEL_STILL_VISIBLE",
                result.code());
        assertEquals(1, port.executionCount);
        assertEquals(0, port.clickCalls);
    }

    @Test
    void invalidPayloadFailsBeforeTheCombatMechanicRuns() {
        RecordingPort port = new RecordingPort();

        LocalServiceExecution ordinaryWithPoint = dispatcher(port).execute(
                call(new TurnXinshouMechanicalArguments(
                        TurnXinshouMechanicalAction.PRESS_ORDINARY_AUTO_COMBAT,
                        null,
                        321, 654,
                        100, 200, 1024, 768)),
                0, null, null, () -> true, "action-1", "device-1", "window-1", null);
        LocalServiceExecution captureWithoutPoint = dispatcher(port).execute(
                call(new TurnXinshouMechanicalArguments(
                        TurnXinshouMechanicalAction.CAPTURE_COMBAT,
                        null)),
                0, null, null, () -> true, "action-2", "device-1", "window-1", null);

        assertEquals(TurnStepResult.Status.FAILED, ordinaryWithPoint.status());
        assertEquals("INVALID_XINSHOU_MECHANICAL_CALL", ordinaryWithPoint.code());
        assertEquals(TurnStepResult.Status.FAILED, captureWithoutPoint.status());
        assertEquals("INVALID_XINSHOU_MECHANICAL_CALL", captureWithoutPoint.code());
        assertEquals(0, port.executionCount);
    }

    @Test
    void captureSizeChangeReturnsTypedFailureWithoutKeyboardOrMouseInput() {
        RecordingPort port = new RecordingPort();
        port.windowWidth = 1000;

        LocalServiceExecution result = dispatcher(port).execute(
                call(captureArguments()),
                0, null, null, () -> true, "action-1", "device-1", "window-1", null);

        assertEquals(TurnStepResult.Status.FAILED, result.status());
        assertEquals(
                "XINSHOU_MECHANICAL_CAPTURE_COMBAT_WINDOW_SIZE_CHANGED",
                result.code());
        assertEquals(List.of("FOCUSED"), port.events);
        assertEquals(1, port.executionCount);
        assertEquals(0, port.clickCalls);
    }

    private static TurnXinshouMechanicalArguments captureArguments() {
        return new TurnXinshouMechanicalArguments(
                TurnXinshouMechanicalAction.CAPTURE_COMBAT,
                null,
                321, 654,
                100, 200, 1024, 768);
    }

    private static Path connectivityDirectory() {
        String configured = System.getenv("DHXY_CONNECTIVITY_DIR");
        if (configured == null || configured.isBlank()) {
            return Path.of("target", "connectivity").toAbsolutePath().normalize();
        }
        return Path.of(configured).toAbsolutePath().normalize();
    }

    private static TurnLocalServiceCall call(TurnXinshouMechanicalArguments arguments) {
        return new TurnLocalServiceCall(
                TurnLocalOperation.XINSHOU_MECHANICAL_ACTION,
                arguments);
    }

    private static LocalServiceStepDispatcher dispatcher(RecordingPort port) {
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
                new XinshouCombatLocalMechanics(port),
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

    private static final class RecordingPort
            implements XinshouCombatLocalMechanics.ExactWindowPort,
            XinshouCombatLocalMechanics.ExactWindowSession {

        private final List<String> events = new ArrayList<>();
        private XinshouCombatLocalMechanics.PanelVisibility panelVisibility =
                XinshouCombatLocalMechanics.PanelVisibility.ABSENT;
        private int windowLeft = 300;
        private int windowTop = 400;
        private int windowWidth = 1024;
        private int windowHeight = 768;
        private int executionCount;
        private int clickCalls;

        @Override
        public XinshouCombatLocalMechanics.Result executeBackground(
                String description,
                XinshouCombatLocalMechanics.ExactWindowAction action) {
            executionCount++;
            events.add("BACKGROUND");
            return action.execute(this);
        }

        @Override
        public XinshouCombatLocalMechanics.Result executeFocused(
                String description,
                XinshouCombatLocalMechanics.ExactWindowAction action) {
            executionCount++;
            events.add("FOCUSED");
            return action.execute(this);
        }

        @Override
        public boolean pressAltA() {
            events.add("ALT_A");
            return true;
        }

        @Override
        public boolean pressAltB() {
            events.add("ALT_B");
            return true;
        }

        @Override
        public boolean pressAlt8() {
            events.add("ALT_8");
            return true;
        }

        @Override
        public boolean waitMillis(int millis) {
            events.add("WAIT:" + millis);
            return true;
        }

        @Override
        public XinshouCombatLocalMechanics.PanelVisibility probeAutoRemaining() {
            events.add("PROBE");
            return panelVisibility;
        }

        @Override
        public int windowLeft() {
            return windowLeft;
        }

        @Override
        public int windowTop() {
            return windowTop;
        }

        @Override
        public int windowWidth() {
            return windowWidth;
        }

        @Override
        public int windowHeight() {
            return windowHeight;
        }

        @Override
        public boolean containsScreenPoint(int screenX, int screenY) {
            events.add("CONTAINS:" + screenX + "," + screenY);
            return true;
        }

        @Override
        public boolean clickAbsolute(int screenX, int screenY) {
            clickCalls++;
            events.add("CLICK:" + screenX + "," + screenY);
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
