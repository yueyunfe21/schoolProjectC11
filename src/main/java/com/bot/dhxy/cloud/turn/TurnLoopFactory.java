package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;

import java.util.Objects;
import java.util.function.Supplier;

/** Explicit construction boundary for inert per-window turn loops. */
public final class TurnLoopFactory {

    private final TurnClient turnClient;
    private final LocalTurnActionExecutor actionExecutor;

    public TurnLoopFactory(TurnClient turnClient, LocalTurnActionExecutor actionExecutor) {
        this.turnClient = Objects.requireNonNull(turnClient, "turnClient");
        this.actionExecutor = Objects.requireNonNull(actionExecutor, "actionExecutor");
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
        return new WindowTurnLoop(
                deviceId,
                windowId,
                waitTimeoutMs,
                windowMetadataSupplier,
                turnClient,
                actionExecutor);
    }
}
