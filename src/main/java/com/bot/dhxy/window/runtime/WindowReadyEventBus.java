package com.bot.dhxy.window.runtime;

import com.bot.dhxy.window.model.WindowReadyEvent;
import com.bot.dhxy.window.model.WindowReadyEventType;
import com.bot.dhxy.window.model.WindowPathingState;
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

    private final Object monitor = new Object();
    private final AtomicLong sequence = new AtomicLong();
    private final Map<String, WindowReadyEvent> latestByWindowAndType = new ConcurrentHashMap<>();
    private final Map<String, WindowReadyEvent> latestPreparedActionByWindow = new ConcurrentHashMap<>();

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
        if (stored.getType() == WindowReadyEventType.TASK_ATTENTION_REQUIRED
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

    /**
     * Return the newest fresh event owned by another window.
     *
     * <p>This remains a wake hint, not business state. Task code uses it only to yield expensive
     * foreground OCR/retry work so the window that already has a visible dialog can be scheduled
     * sooner; the eventual click still has to validate {@link WindowRuntimeContext} state.</p>
     *
     * @param currentWindowId window that is about to run normal work.
     * @param type ready event type to inspect.
     * @param taskType optional task type filter; null accepts any task type.
     * @param maxAgeMs maximum event age in milliseconds.
     * @return newest matching event from a different window, if still fresh.
     */
    public Optional<WindowReadyEvent> latestOtherFresh(String currentWindowId,
                                                       WindowReadyEventType type,
                                                       TaskType taskType,
                                                       long maxAgeMs) {
        if (currentWindowId == null || type == null || maxAgeMs < 0L) {
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        WindowReadyEvent newest = null;
        for (WindowReadyEvent event : latestPreparedActionByWindow.values()) {
            if (event == null
                    || event.getWindowId() == null
                    || event.getWindowId().equals(currentWindowId)
                    || event.getType() != type
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
        for (WindowReadyEvent event : latestByWindowAndType.values()) {
            if (event == null
                    || event.getWindowId() == null
                    || event.getWindowId().equals(currentWindowId)
                    || event.getType() != WindowReadyEventType.TASK_ATTENTION_REQUIRED
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
