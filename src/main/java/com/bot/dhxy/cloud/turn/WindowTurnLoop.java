package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnAction;
import com.bot.dhxy.cloud.turn.protocol.TurnOutcome;
import com.bot.dhxy.cloud.turn.protocol.TurnProtocolValidator;
import com.bot.dhxy.cloud.turn.protocol.TurnRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnResponse;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/** Explicit long-wait turn lifecycle for one immutable device/window identity. */
public final class WindowTurnLoop {

    private static final Logger log = LoggerFactory.getLogger(WindowTurnLoop.class);
    private static final int CONTRACT_VERSION = 1;

    private final Object lifecycleMonitor = new Object();
    private final String deviceId;
    private final String windowId;
    private final long waitTimeoutMs;
    private final Supplier<TurnWindowMetadata> windowMetadataSupplier;
    private final TurnClient turnClient;
    private final LocalTurnActionExecutor actionExecutor;
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile Thread workerThread;
    private volatile Throwable lastFailure;
    private boolean retired;

    // These values remain in memory across an explicitly restarted loop so uncertain transport never re-executes.
    private TurnOutcome previousOutcome;
    private byte[] previousPng;
    private String lastExecutedActionId;
    private ExecutedTurn lastExecutedTurn;

    WindowTurnLoop(String deviceId,
                   String windowId,
                   long waitTimeoutMs,
                   Supplier<TurnWindowMetadata> windowMetadataSupplier,
                   TurnClient turnClient,
                   LocalTurnActionExecutor actionExecutor) {
        this.deviceId = requireIdentity(deviceId, "deviceId");
        this.windowId = requireIdentity(windowId, "windowId");
        if (waitTimeoutMs <= 0L) {
            throw new IllegalArgumentException("waitTimeoutMs must be positive for long-wait exchange");
        }
        this.waitTimeoutMs = waitTimeoutMs;
        this.windowMetadataSupplier = Objects.requireNonNull(windowMetadataSupplier, "windowMetadataSupplier");
        this.turnClient = Objects.requireNonNull(turnClient, "turnClient");
        this.actionExecutor = Objects.requireNonNull(actionExecutor, "actionExecutor");
    }

    /** Starts this window's explicit daemon loop without clearing in-memory acknowledgement state. */
    public void start() {
        synchronized (lifecycleMonitor) {
            if (retired) {
                throw new IllegalStateException("turn loop is permanently retired for windowId=" + windowId);
            }
            if (!running.compareAndSet(false, true)) {
                throw new IllegalStateException("turn loop is already running for windowId=" + windowId);
            }
            stopRequested.set(false);
            lastFailure = null;
            Thread thread = new Thread(this::runLoop, "dhxy-turn-" + windowId);
            thread.setDaemon(true);
            workerThread = thread;
            try {
                thread.start();
            } catch (RuntimeException | Error startFailure) {
                workerThread = null;
                running.set(false);
                lifecycleMonitor.notifyAll();
                throw startFailure;
            }
        }
    }

    /** Requests cooperative stop and interrupts the current long-wait or local action. */
    public void stop() {
        synchronized (lifecycleMonitor) {
            stopRequested.set(true);
            Thread thread = workerThread;
            if (thread != null) {
                thread.interrupt();
            }
        }
    }

    /**
     * Waits for the explicitly started loop thread to stop.
     *
     * @param timeout positive maximum duration to wait
     * @return true when no loop thread is running before the timeout, otherwise false
     * @throws InterruptedException when the waiting caller is interrupted
     */
    public boolean awaitStopped(Duration timeout) throws InterruptedException {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        synchronized (lifecycleMonitor) {
            while (running.get()) {
                long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos <= 0L) {
                    return false;
                }
                long waitMillis = Math.max(1L, Duration.ofNanos(remainingNanos).toMillis());
                lifecycleMonitor.wait(waitMillis);
            }
            return true;
        }
    }

    public String deviceId() {
        return deviceId;
    }

    public String windowId() {
        return windowId;
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isStopRequested() {
        return stopRequested.get();
    }

    public Throwable lastFailure() {
        return lastFailure;
    }

    /** Permanently prevents any later start, or fails when start already won the lifecycle race. */
    void retireIfStopped() {
        synchronized (lifecycleMonitor) {
            if (running.get()) {
                throw new IllegalStateException("cannot retire running turn loop for windowId=" + windowId);
            }
            retired = true;
        }
    }

    private void runLoop() {
        log.info("Turn loop started: deviceId={} windowId={}", deviceId, windowId);
        try {
            while (!stopRequested.get() && !Thread.currentThread().isInterrupted()) {
                exchangeOnce();
            }
        } catch (TurnTransportException transportFailure) {
            if (!stopRequested.get() && !Thread.currentThread().isInterrupted()) {
                lastFailure = transportFailure;
                log.error(
                        "Turn loop stopped after transport failure: deviceId={} windowId={} kind={} message={}",
                        deviceId,
                        windowId,
                        transportFailure.kind(),
                        transportFailure.getMessage());
            }
        } catch (RuntimeException localFailure) {
            if (!stopRequested.get() && !Thread.currentThread().isInterrupted()) {
                lastFailure = localFailure;
                log.error(
                        "Turn loop stopped after local failure: deviceId={} windowId={} type={} message={}",
                        deviceId,
                        windowId,
                        localFailure.getClass().getSimpleName(),
                        localFailure.getMessage());
            }
        } finally {
            synchronized (lifecycleMonitor) {
                workerThread = null;
                running.set(false);
                lifecycleMonitor.notifyAll();
            }
            log.info(
                    "Turn loop stopped: deviceId={} windowId={} stopRequested={} failed={}",
                    deviceId,
                    windowId,
                    stopRequested.get(),
                    lastFailure != null);
        }
    }

    private void exchangeOnce() throws TurnTransportException {
        TurnWindowMetadata metadata = Objects.requireNonNull(
                windowMetadataSupplier.get(),
                "windowMetadataSupplier returned null");
        requireExpectedIdentity(metadata.deviceId(), metadata.windowId(), "request metadata");

        TurnRequest request = TurnProtocolValidator.requireValid(new TurnRequest(
                CONTRACT_VERSION,
                metadata,
                waitTimeoutMs,
                previousOutcome));
        byte[] pngForRequest = previousPng == null ? null : previousPng.clone();

        // Exactly one exchange is attempted. A thrown transport failure leaves both retained values untouched.
        TurnExchangeResult exchangeResult = Objects.requireNonNull(
                turnClient.exchange(request, pngForRequest),
                "turnClient returned null exchange result");

        // Every successful ACTION or IDLE response accepts the exact previous result carried by this request.
        previousOutcome = null;
        previousPng = null;

        TurnResponse response = Objects.requireNonNull(exchangeResult.response(), "turn response");
        if (response.status() == null) {
            throw new IllegalStateException("turn response status must not be null");
        }
        if (stopRequested.get() || Thread.currentThread().isInterrupted()) {
            return;
        }
        if (response.status() == TurnResponse.Status.IDLE) {
            if (response.action() != null) {
                throw new IllegalStateException("IDLE turn response must not contain an action");
            }
            return;
        }

        TurnAction action = TurnProtocolValidator.requireValid(Objects.requireNonNull(
                response.action(),
                "ACTION turn response must contain an action"));
        requireExpectedIdentity(action.deviceId(), action.windowId(), "turn action");

        ExecutedTurn executed;
        if (lastExecutedActionId != null && lastExecutedTurn == null) {
            throw new IllegalStateException(
                    "prior action execution is uncertain; refusing further physical execution");
        }
        if (action.actionId().equals(lastExecutedActionId)) {
            executed = Objects.requireNonNull(lastExecutedTurn, "cached turn missing for repeated actionId");
        } else {
            // Reaching a different action here proves the prior request received a successful acknowledgement.
            lastExecutedActionId = action.actionId();
            lastExecutedTurn = null;
            executed = Objects.requireNonNull(actionExecutor.execute(action), "actionExecutor returned null");
            requireExecutedCorrelation(action, executed.outcome());
            lastExecutedTurn = executed;
        }

        previousOutcome = executed.outcome();
        previousPng = executed.optionalPng();
    }

    private void requireExpectedIdentity(String actualDeviceId, String actualWindowId, String source) {
        if (!deviceId.equals(actualDeviceId) || !windowId.equals(actualWindowId)) {
            throw new IllegalStateException(source + " does not match immutable loop deviceId/windowId");
        }
    }

    private void requireExecutedCorrelation(TurnAction action, TurnOutcome outcome) {
        TurnOutcome validated = TurnProtocolValidator.requireValid(Objects.requireNonNull(outcome, "outcome"));
        requireExpectedIdentity(validated.window().deviceId(), validated.window().windowId(), "turn outcome");
        if (!action.actionId().equals(validated.actionId())) {
            throw new IllegalStateException("turn outcome actionId does not match executed action");
        }
    }

    private static String requireIdentity(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must be nonblank");
        }
        return value;
    }
}
