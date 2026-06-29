package com.bot.dhxy.window.runtime;

import com.bot.dhxy.window.model.WindowReadyEvent;
import com.bot.dhxy.window.model.WindowReadyEventType;
import com.bot.dhxy.window.model.WindowPathingState;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.task.model.TaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory soft wake bus for window observer events.
 *
 * <p>The bus does not own task state and must never trigger input by itself. Watchers publish after
 * updating {@link WindowRuntimeContext}; task code may wait here to wake earlier, then must validate
 * the current runtime snapshot before acting.</p>
 */
@Slf4j
@Component
public class WindowReadyEventBus {

    private static final String RESULT_PAUSE_WAKE = "pause-wake";
    private static final String RESULT_STOP_WAKE = "stop-wake";

    private final Object monitor = new Object();
    private final AtomicLong sequence = new AtomicLong();
    private final Map<String, WindowReadyEvent> latestByWindowAndType = new ConcurrentHashMap<>();
    private final Map<String, WindowReadyEvent> latestPreparedActionByWindow = new ConcurrentHashMap<>();
    private final Map<String, WindowReadyControlWake> latestControlWakeByWindow = new ConcurrentHashMap<>();

    /**
     * Publish a coalesced wake hint for one window.
     *
     * @param event event without a sequence. Null events are ignored.
     * @return the stored event with a monotonically increasing sequence, or empty for null input.
     */
    public Optional<WindowReadyEvent> publish(WindowReadyEvent event) {
        if (event == null || event.getWindowId() == null || event.getType() == null) {
            return Optional.empty();
        }
        WindowReadyEvent stored = event.toBuilder()
                .sequence(sequence.incrementAndGet())
                .createdAtMs(event.getCreatedAtMs() <= 0L ? System.currentTimeMillis() : event.getCreatedAtMs())
                .build();
        latestByWindowAndType.put(stored.eventKey(), stored);
        if (stored.getType() == WindowReadyEventType.PREPARED_ACTION_READY
                && stored.getOperation() != null) {
            latestPreparedActionByWindow.put(stored.getWindowId(), stored);
        }
        synchronized (monitor) {
            monitor.notifyAll();
        }
        long ageMs = Math.max(0L, System.currentTimeMillis() - stored.getCreatedAtMs());
        log.info("[latency] event=window.ready.publish windowId={} hwnd={} type={} task={} source={} operation={} target={} state={} sequence={} createdAtMs={} ageMs={}",
                stored.getWindowId(), stored.getHwnd(), stored.getType(), stored.getTaskType(), stored.getSource(),
                stored.getOperation(), stored.getTargetKeyword(), stored.getPathingState(), stored.getSequence(),
                stored.getCreatedAtMs(), ageMs);
        return Optional.of(stored);
    }

    /**
     * Return the latest published ready-event sequence without blocking.
     *
     * <p>Task code should capture this value immediately before it parks, then call
     * {@link #awaitNewer(String, EnumSet, long, long)} with the captured value. That order makes the
     * wait tolerant to events published right after the task releases its turn.</p>
     *
     * @return latest monotonically increasing ready-event sequence.
     */
    public long currentSequence() {
        return sequence.get();
    }

    /**
     * Wake a task thread parked in a ready-event wait because the user requested pause.
     *
     * <p>This is a control wake only. It deliberately does not publish a {@link WindowReadyEvent},
     * because pause is not a pathing/prepared/combat business fact. The task must return to its
     * normal checkpoint path and let {@code TaskPauseToken} block/resume and report paused time.</p>
     *
     * @param windowId target window id. Null/blank values are ignored.
     * @param reason diagnostic reason for logs; nullable.
     */
    public void wakeForTaskPause(String windowId, String reason) {
        publishControlWake(windowId, RESULT_PAUSE_WAKE, reason);
    }

    /**
     * Wake a task thread parked in a ready-event wait because the user requested stop.
     *
     * <p>This is a control wake only and must not masquerade as a business event. Stop also uses
     * runner-thread interruption; this wake gives the event-wait layer an explicit diagnostic path
     * when it wins the race before the interrupt is observed.</p>
     *
     * @param windowId target window id. Null/blank values are ignored.
     * @param reason diagnostic reason for logs; nullable.
     */
    public void wakeForTaskStop(String windowId, String reason) {
        publishControlWake(windowId, RESULT_STOP_WAKE, reason);
    }

    /**
     * Wait for a newer event for the given window.
     *
     * @param windowId target window id.
     * @param types event types that should wake the caller.
     * @param afterSequence ignore events at or below this sequence.
     * @param timeoutMs maximum wait time in milliseconds; negative means wait until event or
     *                  interruption.
     * @return latest matching event newer than {@code afterSequence}, or empty on timeout/interruption.
     */
    public Optional<WindowReadyEvent> awaitNewer(String windowId,
                                                 EnumSet<WindowReadyEventType> types,
                                                 long afterSequence,
                                                 long timeoutMs) {
        long startedAt = System.currentTimeMillis();
        if (windowId == null || types == null || types.isEmpty() || timeoutMs == 0L) {
            logReadyWait("invalid", windowId, types, afterSequence, timeoutMs,
                    System.currentTimeMillis() - startedAt, Optional.empty());
            return Optional.empty();
        }
        boolean waitUntilEvent = timeoutMs < 0L;
        long deadline = waitUntilEvent ? Long.MAX_VALUE : System.currentTimeMillis() + timeoutMs;
        Optional<WindowReadyControlWake> immediateControl = pollNewerControlWake(windowId, afterSequence);
        if (immediateControl.isPresent()) {
            WindowReadyControlWake wake = immediateControl.get();
            logReadyWait(wake.result(), windowId, types, afterSequence, timeoutMs,
                    System.currentTimeMillis() - startedAt, Optional.empty(), wake);
            return Optional.empty();
        }
        Optional<WindowReadyEvent> immediate = findNewer(windowId, types, afterSequence);
        if (immediate.isPresent()) {
            logReadyWait("event", windowId, types, afterSequence, timeoutMs,
                    System.currentTimeMillis() - startedAt, immediate);
            return immediate;
        }
        synchronized (monitor) {
            while (waitUntilEvent || System.currentTimeMillis() < deadline) {
                long waitMs = waitUntilEvent ? 0L : Math.max(1L, deadline - System.currentTimeMillis());
                try {
                    monitor.wait(waitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logReadyWait("interrupted", windowId, types, afterSequence, timeoutMs,
                            System.currentTimeMillis() - startedAt, Optional.empty());
                    return Optional.empty();
                }
                Optional<WindowReadyControlWake> controlWake = pollNewerControlWake(windowId, afterSequence);
                if (controlWake.isPresent()) {
                    WindowReadyControlWake wake = controlWake.get();
                    logReadyWait(wake.result(), windowId, types, afterSequence, timeoutMs,
                            System.currentTimeMillis() - startedAt, Optional.empty(), wake);
                    return Optional.empty();
                }
                Optional<WindowReadyEvent> event = findNewer(windowId, types, afterSequence);
                if (event.isPresent()) {
                    logReadyWait("event", windowId, types, afterSequence, timeoutMs,
                            System.currentTimeMillis() - startedAt, event);
                    return event;
                }
            }
        }
        if (!waitUntilEvent) {
            logReadyWait("timeout", windowId, types, afterSequence, timeoutMs,
                    System.currentTimeMillis() - startedAt, Optional.empty());
        }
        return Optional.empty();
    }

    /**
     * Wait for a newer terminal pathing event that belongs to one exact pathing intent.
     *
     * @param windowId target window id.
     * @param expectedIntentId pathing intent id that is allowed to wake the caller.
     * @param expectedSourcePrefix optional diagnostic source prefix fallback when the intent id is
     *                             unavailable.
     * @param expectedTargetMapName optional target map fallback when the intent id is unavailable.
     * @param afterSequence ignore events at or below this sequence.
     * @param timeoutMs maximum wait time in milliseconds; negative means wait until event or
     *                  interruption.
     * @return matching terminal event newer than {@code afterSequence}, or empty on timeout.
     */
    public Optional<WindowReadyEvent> awaitNewerPathingTerminal(String windowId,
                                                                String expectedIntentId,
                                                                String expectedSourcePrefix,
                                                                String expectedTargetMapName,
                                                                long afterSequence,
                                                                long timeoutMs) {
        long startedAt = System.currentTimeMillis();
        if (windowId == null || expectedIntentId == null || expectedIntentId.isBlank() || timeoutMs == 0L) {
            logReadyWait("invalid-pathing-terminal", windowId,
                    EnumSet.of(WindowReadyEventType.PATHING_TERMINAL), afterSequence, timeoutMs,
                    System.currentTimeMillis() - startedAt, Optional.empty());
            return Optional.empty();
        }
        boolean waitUntilEvent = timeoutMs < 0L;
        long deadline = waitUntilEvent ? Long.MAX_VALUE : System.currentTimeMillis() + timeoutMs;
        Optional<WindowReadyControlWake> immediateControl = pollNewerControlWake(windowId, afterSequence);
        if (immediateControl.isPresent()) {
            WindowReadyControlWake wake = immediateControl.get();
            logReadyWait(wake.result(), windowId, EnumSet.of(WindowReadyEventType.PATHING_TERMINAL),
                    afterSequence, timeoutMs, System.currentTimeMillis() - startedAt, Optional.empty(), wake);
            return Optional.empty();
        }
        Optional<WindowReadyEvent> immediate = findNewerPathingTerminal(
                windowId, expectedIntentId, expectedSourcePrefix, expectedTargetMapName, afterSequence);
        if (immediate.isPresent()) {
            logReadyWait("event", windowId, EnumSet.of(WindowReadyEventType.PATHING_TERMINAL),
                    afterSequence, timeoutMs, System.currentTimeMillis() - startedAt, immediate);
            return immediate;
        }
        synchronized (monitor) {
            while (waitUntilEvent || System.currentTimeMillis() < deadline) {
                long waitMs = waitUntilEvent ? 0L : Math.max(1L, deadline - System.currentTimeMillis());
                try {
                    monitor.wait(waitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logReadyWait("interrupted", windowId, EnumSet.of(WindowReadyEventType.PATHING_TERMINAL),
                            afterSequence, timeoutMs, System.currentTimeMillis() - startedAt, Optional.empty());
                    return Optional.empty();
                }
                Optional<WindowReadyControlWake> controlWake = pollNewerControlWake(windowId, afterSequence);
                if (controlWake.isPresent()) {
                    WindowReadyControlWake wake = controlWake.get();
                    logReadyWait(wake.result(), windowId, EnumSet.of(WindowReadyEventType.PATHING_TERMINAL),
                            afterSequence, timeoutMs, System.currentTimeMillis() - startedAt,
                            Optional.empty(), wake);
                    return Optional.empty();
                }
                Optional<WindowReadyEvent> event = findNewerPathingTerminal(
                        windowId, expectedIntentId, expectedSourcePrefix, expectedTargetMapName, afterSequence);
                if (event.isPresent()) {
                    logReadyWait("event", windowId, EnumSet.of(WindowReadyEventType.PATHING_TERMINAL),
                            afterSequence, timeoutMs, System.currentTimeMillis() - startedAt, event);
                    return event;
                }
            }
        }
        if (!waitUntilEvent) {
            logReadyWait("timeout", windowId, EnumSet.of(WindowReadyEventType.PATHING_TERMINAL),
                    afterSequence, timeoutMs, System.currentTimeMillis() - startedAt, Optional.empty());
        }
        return Optional.empty();
    }

    /**
     * Wait for either a terminal pathing event for one pathing intent or a prepared route dialog
     * for the same destination.
     *
     * <p>修罗 target navigation can stop at a route-transfer dialog before the pathing watcher
     * publishes a terminal state. The prepared route action is a business-relevant wake hint for
     * the same phase, but it still does not execute input here; the task wakes and re-enters its
     * normal navigation consumer.</p>
     *
     * @param windowId target window id.
     * @param expectedIntentId pathing intent id allowed for terminal events.
     * @param expectedSourcePrefix optional diagnostic source prefix kept for parity with terminal waits.
     * @param expectedTargetMapName expected route destination for prepared route-dialog events.
     * @param afterSequence ignore events at or below this sequence.
     * @param timeoutMs maximum wait time in milliseconds; negative means wait until event or
     *                  interruption.
     * @return matching terminal or prepared-route event newer than {@code afterSequence}, or empty
     *         on timeout.
     */
    public Optional<WindowReadyEvent> awaitNewerPathingTerminalOrPreparedRoute(String windowId,
                                                                               String expectedIntentId,
                                                                               String expectedSourcePrefix,
                                                                               String expectedTargetMapName,
                                                                               long afterSequence,
                                                                               long timeoutMs) {
        EnumSet<WindowReadyEventType> types = EnumSet.of(
                WindowReadyEventType.PATHING_TERMINAL,
                WindowReadyEventType.PREPARED_ACTION_READY);
        long startedAt = System.currentTimeMillis();
        if (windowId == null || timeoutMs == 0L) {
            logReadyWait("invalid-pathing-or-prepared-route", windowId, types, afterSequence, timeoutMs,
                    System.currentTimeMillis() - startedAt, Optional.empty());
            return Optional.empty();
        }
        boolean waitUntilEvent = timeoutMs < 0L;
        long deadline = waitUntilEvent ? Long.MAX_VALUE : System.currentTimeMillis() + timeoutMs;
        Optional<WindowReadyControlWake> immediateControl = pollNewerControlWake(windowId, afterSequence);
        if (immediateControl.isPresent()) {
            WindowReadyControlWake wake = immediateControl.get();
            logReadyWait(wake.result(), windowId, types, afterSequence, timeoutMs,
                    System.currentTimeMillis() - startedAt, Optional.empty(), wake);
            return Optional.empty();
        }
        Optional<WindowReadyEvent> immediate = findNewerPathingTerminalOrPreparedRoute(
                windowId, expectedIntentId, expectedSourcePrefix, expectedTargetMapName, afterSequence);
        if (immediate.isPresent()) {
            logReadyWait("event", windowId, types, afterSequence, timeoutMs,
                    System.currentTimeMillis() - startedAt, immediate);
            return immediate;
        }
        synchronized (monitor) {
            while (waitUntilEvent || System.currentTimeMillis() < deadline) {
                long waitMs = waitUntilEvent ? 0L : Math.max(1L, deadline - System.currentTimeMillis());
                try {
                    monitor.wait(waitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logReadyWait("interrupted", windowId, types, afterSequence, timeoutMs,
                            System.currentTimeMillis() - startedAt, Optional.empty());
                    return Optional.empty();
                }
                Optional<WindowReadyControlWake> controlWake = pollNewerControlWake(windowId, afterSequence);
                if (controlWake.isPresent()) {
                    WindowReadyControlWake wake = controlWake.get();
                    logReadyWait(wake.result(), windowId, types, afterSequence, timeoutMs,
                            System.currentTimeMillis() - startedAt, Optional.empty(), wake);
                    return Optional.empty();
                }
                Optional<WindowReadyEvent> event = findNewerPathingTerminalOrPreparedRoute(
                        windowId, expectedIntentId, expectedSourcePrefix, expectedTargetMapName, afterSequence);
                if (event.isPresent()) {
                    logReadyWait("event", windowId, types, afterSequence, timeoutMs,
                            System.currentTimeMillis() - startedAt, event);
                    return event;
                }
            }
        }
        if (!waitUntilEvent) {
            logReadyWait("timeout", windowId, types, afterSequence, timeoutMs,
                    System.currentTimeMillis() - startedAt, Optional.empty());
        }
        return Optional.empty();
    }

    /**
     * Return the latest event for one window/type without blocking.
     *
     * @param windowId target window id.
     * @param type event type.
     * @return latest stored event, if any.
     */
    public Optional<WindowReadyEvent> latest(String windowId, WindowReadyEventType type) {
        if (windowId == null || type == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(latestByWindowAndType.get(windowId + ":" + type));
    }

    /**
     * Return the newest fresh prepared-action signal owned by another window.
     *
     * <p>Plain visible STORY/OPTION events are not enough to preempt other windows: they may still
     * need OCR or business validation. Events with an operation already have a prepared click target
     * such as a route option or task-tracker green link, so task code may yield to them before the
     * cached validation expires.</p>
     *
     * @param currentWindowId window that is about to run normal work.
     * @param taskType optional task type filter; null accepts any task type.
     * @param maxAgeMs maximum event age in milliseconds.
     * @return newest fresh prepared-action event from a different window.
     */
    public Optional<WindowReadyEvent> latestOtherFreshPreparedAction(String currentWindowId,
                                                                     TaskType taskType,
                                                                     long maxAgeMs) {
        if (currentWindowId == null || maxAgeMs < 0L) {
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        WindowReadyEvent newest = null;
        for (WindowReadyEvent event : latestPreparedActionByWindow.values()) {
            if (event == null
                    || event.getWindowId() == null
                    || event.getWindowId().equals(currentWindowId)
                    || event.getType() != WindowReadyEventType.PREPARED_ACTION_READY
                    || event.getOperation() == null
                    || (taskType != null && event.getTaskType() != taskType)
                    || now - event.getCreatedAtMs() > maxAgeMs) {
                continue;
            }
            if (newest == null || event.getSequence() > newest.getSequence()) {
                newest = event;
            }
        }
        return Optional.ofNullable(newest);
    }

    /**
     * Return the newest fresh pathing-terminal signal owned by another window.
     *
     * <p>This is lower priority than a prepared click action. It is still important because ARRIVED
     * and STOPPED_AWAY mean the task has stopped doing useful background movement and should resume
     * its phase before ordinary windows keep scanning.</p>
     *
     * @param currentWindowId window that is about to run normal work.
     * @param taskType optional task type filter; null accepts any task type.
     * @param maxAgeMs maximum event age in milliseconds.
     * @return newest fresh terminal event from a different window.
     */
    public Optional<WindowReadyEvent> latestOtherFreshPathingTerminal(String currentWindowId,
                                                                      TaskType taskType,
                                                                      long maxAgeMs) {
        if (currentWindowId == null || maxAgeMs < 0L) {
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        WindowReadyEvent newest = null;
        for (WindowReadyEvent event : latestByWindowAndType.values()) {
            if (event == null
                    || event.getWindowId() == null
                    || event.getWindowId().equals(currentWindowId)
                    || event.getType() != WindowReadyEventType.PATHING_TERMINAL
                    || (event.getPathingState() != WindowPathingState.ARRIVED
                            && event.getPathingState() != WindowPathingState.STOPPED_AWAY)
                    || (taskType != null && event.getTaskType() != taskType)
                    || now - event.getCreatedAtMs() > maxAgeMs) {
                continue;
            }
            if (newest == null || event.getSequence() > newest.getSequence()) {
                newest = event;
            }
        }
        return Optional.ofNullable(newest);
    }

    private void publishControlWake(String windowId, String result, String reason) {
        String safeWindowId = normalize(windowId);
        if (safeWindowId == null) {
            return;
        }
        WindowReadyControlWake wake = new WindowReadyControlWake(
                safeWindowId,
                result,
                normalize(reason),
                sequence.incrementAndGet(),
                System.currentTimeMillis());
        latestControlWakeByWindow.put(safeWindowId, wake);
        synchronized (monitor) {
            monitor.notifyAll();
        }
        log.info("[latency] event=window.ready.control-wake result={} windowId={} sequence={} reason={} createdAtMs={}",
                wake.result(), wake.windowId(), wake.sequence(), wake.reason(), wake.createdAtMs());
    }

    private Optional<WindowReadyControlWake> pollNewerControlWake(String windowId, long afterSequence) {
        if (windowId == null) {
            return Optional.empty();
        }
        WindowReadyControlWake wake = latestControlWakeByWindow.get(windowId);
        if (wake == null || wake.sequence() <= afterSequence) {
            return Optional.empty();
        }
        latestControlWakeByWindow.remove(windowId, wake);
        return Optional.of(wake);
    }

    private Optional<WindowReadyEvent> findNewerPathingTerminal(String windowId,
                                                                String expectedIntentId,
                                                                String expectedSourcePrefix,
                                                                String expectedTargetMapName,
                                                                long afterSequence) {
        WindowReadyEvent event = latestByWindowAndType.get(windowId + ":" + WindowReadyEventType.PATHING_TERMINAL);
        if (event == null
                || event.getSequence() <= afterSequence
                || event.getPathingState() != WindowPathingState.ARRIVED
                        && event.getPathingState() != WindowPathingState.STOPPED_AWAY
                || event.getPathingIntent() == null
                || event.getPathingIntent().getIntentId() == null
                || !event.getPathingIntent().getIntentId().equals(expectedIntentId)) {
            return Optional.empty();
        }
        return Optional.of(event);
    }

    private Optional<WindowReadyEvent> findNewerPathingTerminalOrPreparedRoute(String windowId,
                                                                               String expectedIntentId,
                                                                               String expectedSourcePrefix,
                                                                               String expectedTargetMapName,
                                                                               long afterSequence) {
        Optional<WindowReadyEvent> terminal = findNewerPathingTerminal(
                windowId, expectedIntentId, expectedSourcePrefix, expectedTargetMapName, afterSequence);
        WindowReadyEvent prepared = latestByWindowAndType.get(
                windowId + ":" + WindowReadyEventType.PREPARED_ACTION_READY);
        if (prepared == null
                || prepared.getSequence() <= afterSequence
                || prepared.getOperation() != DialogOperation.ROUTE_TRANSFER
                || expectedTargetMapName == null
                || expectedTargetMapName.isBlank()
                || prepared.getTargetKeyword() == null
                || !prepared.getTargetKeyword().equals(expectedTargetMapName)) {
            return terminal;
        }
        if (terminal.isPresent() && terminal.get().getSequence() > prepared.getSequence()) {
            return terminal;
        }
        return Optional.of(prepared);
    }

    private Optional<WindowReadyEvent> findNewer(String windowId,
                                                 EnumSet<WindowReadyEventType> types,
                                                 long afterSequence) {
        WindowReadyEvent newest = null;
        for (WindowReadyEventType type : types) {
            WindowReadyEvent event = latestByWindowAndType.get(windowId + ":" + type);
            if (event != null
                    && event.getSequence() > afterSequence
                    && (newest == null || event.getSequence() > newest.getSequence())) {
                newest = event;
            }
        }
        return Optional.ofNullable(newest);
    }

    private void logReadyWait(String result,
                              String windowId,
                              EnumSet<WindowReadyEventType> types,
                              long afterSequence,
                              long timeoutMs,
                              long elapsedMs,
                              Optional<WindowReadyEvent> event) {
        logReadyWait(result, windowId, types, afterSequence, timeoutMs, elapsedMs, event, null);
    }

    private void logReadyWait(String result,
                              String windowId,
                              EnumSet<WindowReadyEventType> types,
                              long afterSequence,
                              long timeoutMs,
                              long elapsedMs,
                              Optional<WindowReadyEvent> event,
                              WindowReadyControlWake controlWake) {
        WindowReadyEvent ready = event.orElse(null);
        log.info("[latency] event=window.ready.await result={} windowId={} wakeTypes={} afterSequence={} returnedSequence={} returnedType={} controlSequence={} controlReason={} timeoutMs={} elapsedMs={} wokeByEvent={} wokeByTimeout={} wokeByControl={}",
                result, windowId, types, afterSequence,
                ready == null ? null : ready.getSequence(),
                ready == null ? null : ready.getType(),
                controlWake == null ? null : controlWake.sequence(),
                controlWake == null ? null : controlWake.reason(),
                timeoutMs, Math.max(0L, elapsedMs),
                ready != null, "timeout".equals(result), controlWake != null);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record WindowReadyControlWake(String windowId,
                                          String result,
                                          String reason,
                                          long sequence,
                                          long createdAtMs) {
    }
}
