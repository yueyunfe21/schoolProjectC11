package com.bot.dhxy.window.control;

import com.bot.dhxy.cloud.turn.protocol.TurnTaskCode;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskQueueFailurePolicy;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskStartAck;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.execution.WindowTaskFailurePolicy;
import com.bot.dhxy.window.execution.WindowTaskQueue;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TURN-40D contract test for the DHXY-side remote turn control authority the control service owns before any loop is
 * started: exactly which local task types may become a remote {@code TurnTaskStartRequest}, and how a window's live
 * remote {@link TurnWindowMetadata} is projected from its real registered baseline. These are the control service's own
 * responsibilities and are proven directly, with no window loop, no thread and no physical input.
 *
 * <p>The complementary guard-lifecycle and loop behavioural proofs — the four-argument guard start, local/remote mutex
 * at activation, immutable start attach/resend until a matching ack, pause/resume without reminting a start, same-guard
 * stop publishing exactly one final stop-bearing turn then unregistering (zero returned actions executed), and the
 * public {@code WindowTaskControlService} remote entry points end to end — require the package-private
 * {@code WindowTurnLoop} constructor and the observable-executor harness ({@code TurnContractFixtures}) that live in
 * package {@code com.bot.dhxy.cloud.turn}. Per the R2 write-set expansion they are proven in
 * {@code WindowTurnLoopContractTest} (the second class of the authorized named family); this file proves the control
 * service's own authority contract (task-code mapping and metadata authority projection).
 */
class WindowRemoteTurnControlContractTest {

    private static final String DEVICE_ID = "device-remote-1";
    private static final String WINDOW_ID = "window-remote-1";

    // ---- authority mapping: exactly the four wire task codes, everything else rejected --------------------------

    @Test
    void everySupportedLocalTaskTypeMapsToItsExactWireCode() {
        assertEquals(TurnTaskCode.WUHUAN_V2, WindowTaskControlService.toTurnTaskCode(TaskType.WUHuan_V2));
        assertEquals(TurnTaskCode.WUBEI, WindowTaskControlService.toTurnTaskCode(TaskType.WUBEI));
        assertEquals(TurnTaskCode.XIULUO_V2, WindowTaskControlService.toTurnTaskCode(TaskType.XIULUO_V2));
        assertEquals(TurnTaskCode.AUTO_BATTLE, WindowTaskControlService.toTurnTaskCode(TaskType.AUTO_BATTLE));
    }

    @Test
    void everyUnsupportedLocalTaskTypeIsRejectedForRemoteTurn() {
        // Legacy v1 修罗, the local-only sleep task, and the unknown selection carry no remote turn protocol code.
        assertThrows(IllegalArgumentException.class,
                () -> WindowTaskControlService.toTurnTaskCode(TaskType.XIULUO));
        assertThrows(IllegalArgumentException.class,
                () -> WindowTaskControlService.toTurnTaskCode(TaskType.SLEEP_COMPUTER));
        assertThrows(IllegalArgumentException.class,
                () -> WindowTaskControlService.toTurnTaskCode(TaskType.UNKNOWN));
    }

    @Test
    void queueMappingPreservesOrderAndRejectsEmptyOrUnsupportedQueues() {
        List<TurnTaskCode> mapped = WindowTaskControlService.toTurnTaskCodes(
                WindowTaskQueue.of(TaskType.WUHuan_V2, TaskType.AUTO_BATTLE, TaskType.WUBEI));
        assertEquals(List.of(TurnTaskCode.WUHUAN_V2, TurnTaskCode.AUTO_BATTLE, TurnTaskCode.WUBEI), mapped);

        // An empty queue has nothing to run remotely.
        assertThrows(IllegalArgumentException.class,
                () -> WindowTaskControlService.toTurnTaskCodes(WindowTaskQueue.empty()));

        // A single unsupported member rejects the whole queue rather than silently dropping it.
        assertThrows(IllegalArgumentException.class,
                () -> WindowTaskControlService.toTurnTaskCodes(
                        WindowTaskQueue.of(TaskType.WUHuan_V2, TaskType.SLEEP_COMPUTER)));
    }

    @Test
    void failurePolicyMapsStopOnFailureExactlyAndOtherwiseContinues() {
        assertEquals(TurnTaskQueueFailurePolicy.STOP_ON_FAILURE,
                WindowTaskControlService.toTurnFailurePolicy(WindowTaskFailurePolicy.STOP_ON_FAILURE));
        assertEquals(TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE,
                WindowTaskControlService.toTurnFailurePolicy(WindowTaskFailurePolicy.CONTINUE_ON_FAILURE));
    }

    @Test
    void effectiveAckQueueProjectsMemberAutoBattleWithoutChangingFailurePolicy() {
        WindowTaskQueue requested = WindowTaskQueue.of(TaskType.XIULUO_V2)
                .withFailurePolicy(WindowTaskFailurePolicy.STOP_ON_FAILURE);

        WindowTaskQueue effective = WindowTaskControlService.projectEffectiveQueue(requested,
                new TurnTaskStartAck("member-start", List.of(TurnTaskCode.AUTO_BATTLE)));

        assertEquals(List.of(TaskType.AUTO_BATTLE), effective.getTaskTypes());
        assertEquals(WindowTaskFailurePolicy.STOP_ON_FAILURE, effective.getFailurePolicy());
    }

    @Test
    void legacyAckWithoutProjectionKeepsRequestedQueue() {
        WindowTaskQueue requested = WindowTaskQueue.single(TaskType.XIULUO_V2);

        assertSame(requested, WindowTaskControlService.projectEffectiveQueue(
                requested, new TurnTaskStartAck("legacy-start")));
    }

    // ---- metadata authority projection: real baseline, never hardcoded, loop owns pause/stop ---------------------

    @Test
    void metadataSupplierProjectsTheRealRegisteredBaselineWithTeamFactsAbsent() {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW_ID, new GameContext());
        context.setRole(WindowRole.LEADER);
        WindowNativeBinding binding =
                new WindowNativeBinding("0x1234", "大话西游2", "GameWindowClass", 4321L, 10, 20, 800, 600);

        TurnWindowMetadata metadata = new WindowTaskControlService.RemoteTurnMetadataSupplier(
                DEVICE_ID, context, refreshServiceReturning(Optional.of(binding))).get();

        assertEquals(DEVICE_ID, metadata.deviceId());
        assertEquals(WINDOW_ID, metadata.windowId());
        assertEquals(binding.getTitle(), metadata.windowTitle());
        assertEquals(binding.getNativeHandle(), metadata.nativeHandle());
        assertEquals(binding.getProcessId(), metadata.processId());
        assertEquals(10, metadata.windowRect().left());
        assertEquals(20, metadata.windowRect().top());
        assertEquals(800, metadata.windowRect().width());
        assertEquals(600, metadata.windowRect().height());

        // Authority is derived from the real registered baseline, not hardcoded.
        assertEquals(WindowRole.LEADER.name(), metadata.windowRole());

        // A window driven remotely is not running a local team session, so team facts are truthfully absent.
        assertNull(metadata.localTeamSessionKey());
        assertNull(metadata.localLeaderWindowId());
        assertFalse(Boolean.TRUE.equals(metadata.localLeaderPresent()));
        assertFalse(Boolean.TRUE.equals(metadata.localSupportMember()));

        // Startup mode is the ordinary NORMAL projection; pause/stop belong to the live loop, not this snapshot.
        assertEquals("NORMAL", metadata.startupMode());
        assertFalse(metadata.pauseRequested());
        assertFalse(metadata.stopRequested());
        assertNull(metadata.pathingSnapshot());
    }

    @Test
    void metadataSupplierProjectsTheContextRoleVerbatim() {
        WindowRuntimeContext member = new WindowRuntimeContext(WINDOW_ID, new GameContext());
        member.setRole(WindowRole.MEMBER);
        WindowNativeBinding binding =
                new WindowNativeBinding("0x20", "member", "cls", 7L, 0, 0, 1024, 768);

        TurnWindowMetadata metadata = new WindowTaskControlService.RemoteTurnMetadataSupplier(
                DEVICE_ID, member, refreshServiceReturning(Optional.of(binding))).get();

        assertEquals(WindowRole.MEMBER.name(), metadata.windowRole());
    }

    @Test
    void metadataSupplierRejectsAWindowWithoutALiveNativeBinding() {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW_ID, new GameContext());
        WindowTaskControlService.RemoteTurnMetadataSupplier supplier =
                new WindowTaskControlService.RemoteTurnMetadataSupplier(
                        DEVICE_ID, context, refreshServiceReturning(Optional.empty()));

        assertThrows(IllegalStateException.class, supplier::get);
    }

    @Test
    void metadataSupplierRejectsAnIncompleteNativeBinding() {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW_ID, new GameContext());
        // A binding with neither a native handle nor geometry cannot address a physical window.
        WindowTaskControlService.RemoteTurnMetadataSupplier supplier =
                new WindowTaskControlService.RemoteTurnMetadataSupplier(
                        DEVICE_ID, context, refreshServiceReturning(Optional.of(WindowNativeBinding.empty())));

        assertThrows(IllegalStateException.class, supplier::get);
    }

    @Test
    void metadataSupplierRefreshesAgainstTheExactBoundWindowContext() {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW_ID, new GameContext());
        context.setRole(WindowRole.LEADER);
        WindowNativeBinding binding =
                new WindowNativeBinding("0x1", "t", "c", 1L, 1, 1, 2, 2);
        RecordingRefreshService refreshService = new RecordingRefreshService(Optional.of(binding));

        new WindowTaskControlService.RemoteTurnMetadataSupplier(DEVICE_ID, context, refreshService).get();

        assertSame(context, refreshService.lastContext, "the supplier must refresh the exact bound window context");
    }

    // ---- fakes ---------------------------------------------------------------------------------------------------

    private static WindowNativeBindingRefreshService refreshServiceReturning(Optional<WindowNativeBinding> result) {
        return new RecordingRefreshService(result);
    }

    /**
     * Overrides only the refresh seam so the projection can be proven without any native window probe call. The base
     * no-argument constructor is used unchanged; its probe is never invoked because {@code refreshAndCommit} is
     * overridden.
     */
    private static final class RecordingRefreshService extends WindowNativeBindingRefreshService {
        private final Optional<WindowNativeBinding> result;
        private volatile WindowRuntimeContext lastContext;

        private RecordingRefreshService(Optional<WindowNativeBinding> result) {
            this.result = result;
        }

        @Override
        public Optional<WindowNativeBinding> refreshAndCommit(WindowRuntimeContext context) {
            this.lastContext = context;
            return result;
        }
    }
}
