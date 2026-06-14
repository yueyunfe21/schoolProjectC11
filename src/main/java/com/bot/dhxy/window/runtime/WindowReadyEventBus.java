package com.bot.dhxy.window.runtime;

import com.bot.dhxy.window.model.WindowReadyEvent;
import com.bot.dhxy.window.model.WindowReadyEventType;
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

    private final Object monitor = new Object();
    private final AtomicLong sequence = new AtomicLong();
    private final Map<String, WindowReadyEvent> latestByWindowAndType = new ConcurrentHashMap<>();

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
        synchronized (monitor) {
            monitor.notifyAll();
        }
        log.info("[latency] event=window.ready.publish windowId={} type={} task={} source={} state={} sequence={}",
                stored.getWindowId(), stored.getType(), stored.getTaskType(), stored.getSource(),
                stored.getPathingState(), stored.getSequence());
        return Optional.of(stored);
    }

    /**
     * Wait for a newer event for the given window.
     *
     * @param windowId target window id.
     * @param types event types that should wake the caller.
     * @param afterSequence ignore events at or below this sequence.
     * @param timeoutMs maximum wait time in milliseconds.
     * @return latest matching event newer than {@code afterSequence}, or empty on timeout/interruption.
     */
    public Optional<WindowReadyEvent> awaitNewer(String windowId,
                                                 EnumSet<WindowReadyEventType> types,
                                                 long afterSequence,
                                                 long timeoutMs) {
        if (windowId == null || types == null || types.isEmpty() || timeoutMs <= 0L) {
            return Optional.empty();
        }
        long deadline = System.currentTimeMillis() + timeoutMs;
        Optional<WindowReadyEvent> immediate = findNewer(windowId, types, afterSequence);
        if (immediate.isPresent()) {
            return immediate;
        }
        synchronized (monitor) {
            while (System.currentTimeMillis() < deadline) {
                long waitMs = Math.max(1L, deadline - System.currentTimeMillis());
                try {
                    monitor.wait(waitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return Optional.empty();
                }
                Optional<WindowReadyEvent> event = findNewer(windowId, types, afterSequence);
                if (event.isPresent()) {
                    return event;
                }
            }
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
}
