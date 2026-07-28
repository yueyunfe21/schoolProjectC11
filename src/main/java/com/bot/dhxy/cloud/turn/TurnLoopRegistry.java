package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnTaskStartRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** Explicit registry enforcing at most one inert or running loop for each exact window id. */
public final class TurnLoopRegistry {

    private final TurnLoopFactory loopFactory;
    private final Map<String, WindowTurnLoop> loopsByWindowId = new HashMap<>();

    public TurnLoopRegistry(TurnLoopFactory loopFactory) {
        this.loopFactory = Objects.requireNonNull(loopFactory, "loopFactory");
    }

    /**
     * Creates and registers one stopped loop without starting it.
     *
     * @param deviceId immutable nonblank device identity
     * @param windowId exact nonblank registry key and immutable loop identity
     * @param waitTimeoutMs positive Cloud long-wait duration in milliseconds
     * @param windowMetadataSupplier live metadata supplier invoked once before every request
     * @return the newly registered stopped loop
     * @throws IllegalStateException when the exact window id already has a loop
     */
    public synchronized WindowTurnLoop create(String deviceId,
                                              String windowId,
                                              long waitTimeoutMs,
                                              Supplier<TurnWindowMetadata> windowMetadataSupplier) {
        return createInternal(deviceId, windowId, waitTimeoutMs, windowMetadataSupplier, null);
    }

    /**
     * TURN-40D remote overload: creates and registers one stopped loop and attaches the exact immutable
     * {@link TurnTaskStartRequest} before it is started, so the remote start rides every turn until its matching
     * ack. Callers of the four-argument form are unchanged.
     *
     * @param startRequest non-null immutable remote start request carried until acknowledged.
     */
    public synchronized WindowTurnLoop create(String deviceId,
                                              String windowId,
                                              long waitTimeoutMs,
                                              Supplier<TurnWindowMetadata> windowMetadataSupplier,
                                              TurnTaskStartRequest startRequest) {
        return createInternal(deviceId, windowId, waitTimeoutMs, windowMetadataSupplier,
                Objects.requireNonNull(startRequest, "startRequest"));
    }

    private WindowTurnLoop createInternal(String deviceId,
                                          String windowId,
                                          long waitTimeoutMs,
                                          Supplier<TurnWindowMetadata> windowMetadataSupplier,
                                          TurnTaskStartRequest startRequest) {
        requireWindowId(windowId);
        if (loopsByWindowId.containsKey(windowId)) {
            throw new IllegalStateException("turn loop already exists for windowId=" + windowId);
        }
        WindowTurnLoop loop = loopFactory.create(
                deviceId,
                windowId,
                waitTimeoutMs,
                windowMetadataSupplier);
        if (startRequest != null) {
            loop.attachStartRequest(startRequest);
        }
        loopsByWindowId.put(windowId, loop);
        return loop;
    }

    /** Returns the exact registered loop without creating or starting one. */
    public synchronized Optional<WindowTurnLoop> find(String windowId) {
        requireWindowId(windowId);
        return Optional.ofNullable(loopsByWindowId.get(windowId));
    }

    /**
     * Removes an explicitly stopped loop.
     *
     * @param windowId exact nonblank registry key
     * @return the removed stopped loop
     * @throws IllegalStateException when no exact loop exists or that loop is still running
     */
    public synchronized WindowTurnLoop remove(String windowId) {
        requireWindowId(windowId);
        WindowTurnLoop loop = loopsByWindowId.get(windowId);
        if (loop == null) {
            throw new IllegalStateException("no turn loop exists for windowId=" + windowId);
        }
        loop.retireIfStopped();
        loopsByWindowId.remove(windowId);
        return loop;
    }

    public synchronized int size() {
        return loopsByWindowId.size();
    }

    private static void requireWindowId(String windowId) {
        if (windowId == null || windowId.isBlank()) {
            throw new IllegalArgumentException("windowId must be nonblank");
        }
    }
}
