package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
import com.bot.dhxy.window.observation.ObservationRunnerWiring;
import com.bot.dhxy.window.observation.WindowObservationRunnerFactory;

import java.util.Objects;
import java.util.function.Supplier;

/** Explicit construction boundary for inert per-window turn loops. */
public final class TurnLoopFactory {

    private final TurnClient turnClient;
    private final LocalTurnActionExecutor actionExecutor;
    private final WindowObservationRunnerFactory observationRunnerFactory;
    private final TaskQueueEventRecorder taskQueueEventRecorder;

    public TurnLoopFactory(TurnClient turnClient, LocalTurnActionExecutor actionExecutor) {
        this(turnClient, actionExecutor, null, TaskQueueEventRecorder.NO_OP);
    }

    /**
     * TURN-40G overload: additionally threads a per-window observation runner factory into every created loop.
     * The two-argument form resolves the process-wide {@link ObservationRunnerWiring} registration lazily at
     * creation time instead, so processes without an observation plane (contract tests) are unaffected.
     *
     * @param observationRunnerFactory explicit runner factory, or {@code null} to resolve from the wiring bridge.
     */
    public TurnLoopFactory(TurnClient turnClient,
                           LocalTurnActionExecutor actionExecutor,
                           WindowObservationRunnerFactory observationRunnerFactory) {
        this(turnClient, actionExecutor, observationRunnerFactory, TaskQueueEventRecorder.NO_OP);
    }

    public TurnLoopFactory(TurnClient turnClient,
                           LocalTurnActionExecutor actionExecutor,
                           WindowObservationRunnerFactory observationRunnerFactory,
                           TaskQueueEventRecorder taskQueueEventRecorder) {
        this.turnClient = Objects.requireNonNull(turnClient, "turnClient");
        this.actionExecutor = Objects.requireNonNull(actionExecutor, "actionExecutor");
        this.observationRunnerFactory = observationRunnerFactory;
        this.taskQueueEventRecorder = taskQueueEventRecorder == null ? TaskQueueEventRecorder.NO_OP : taskQueueEventRecorder;
    }

    /**
     * Creates one stopped loop. The caller must explicitly register and start it.
     *
     * @param deviceId immutable nonblank device identity
     * @param windowId immutable nonblank logical window identity
     * @param waitTimeoutMs positive Cloud long-wait duration in milliseconds
     * @param windowMetadataSupplier live metadata supplier invoked once before every request
     * @return a stopped loop with no automatic lifecycle hook
     */
    WindowTurnLoop create(String deviceId,
                          String windowId,
                          long waitTimeoutMs,
                          Supplier<TurnWindowMetadata> windowMetadataSupplier) {
        WindowObservationRunnerFactory runnerFactory = observationRunnerFactory != null
                ? observationRunnerFactory
                : ObservationRunnerWiring.current();
        return new WindowTurnLoop(
                deviceId,
                windowId,
                waitTimeoutMs,
                windowMetadataSupplier,
                turnClient,
                Objects.requireNonNull(actionExecutor, "actionExecutor")::execute,
                runnerFactory,
                taskQueueEventRecorder);
    }
}
