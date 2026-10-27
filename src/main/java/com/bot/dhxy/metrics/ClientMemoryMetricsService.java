package com.bot.dhxy.metrics;

import com.bot.dhxy.input.action.InputActionQueue;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Low-frequency, read-only JVM memory diagnostics for the desktop client.
 *
 * <p>This service never captures a frame, scans files, requests input, calls {@code System.gc()}, or changes task
 * state. It reads MXBeans and existing O(1) counters only, so task lifecycle logging cannot become a new hot-path
 * workload.</p>
 */
@Service
@Slf4j
public class ClientMemoryMetricsService {

    private static final long PERIOD_MINUTES = 5L;
    private static final double BYTES_PER_MIB = 1024.0d * 1024.0d;

    private final InputActionQueue inputActionQueue;
    private final ObjectProvider<MultiWindowTaskManager> taskManagerProvider;
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final List<BufferPoolMXBean> bufferPools =
            ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "dhxy-client-memory-metrics");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    public ClientMemoryMetricsService(InputActionQueue inputActionQueue,
                                      ObjectProvider<MultiWindowTaskManager> taskManagerProvider) {
        this.inputActionQueue = inputActionQueue;
        this.taskManagerProvider = taskManagerProvider;
    }

    @PostConstruct
    void schedulePeriodicSnapshot() {
        scheduler.scheduleWithFixedDelay(
                () -> logSnapshot("periodic", null),
                PERIOD_MINUTES,
                PERIOD_MINUTES,
                TimeUnit.MINUTES);
    }

    @EventListener(ApplicationReadyEvent.class)
    void onApplicationReady() {
        logSnapshot("application-ready", null);
    }

    /**
     * Records one task lifecycle boundary after the owning runner has committed its new state.
     *
     * @param trigger stable diagnostic trigger such as {@code task-start}, {@code task-stop}, or {@code task-pause}.
     * @param windowId exact local window id, or null for a process-wide snapshot.
     */
    public void logSnapshot(String trigger, String windowId) {
        try {
            writeSnapshot(trigger, windowId);
        } catch (RuntimeException failure) {
            // Diagnostics must never change a task lifecycle outcome.
            log.warn("[client-memory] snapshot failed: trigger={} windowId={} reason={}",
                    normalized(trigger), normalized(windowId), failure.getMessage());
        }
    }

    private void writeSnapshot(String trigger, String windowId) {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
        BufferPoolSnapshot direct = bufferPool("direct");
        BufferPoolSnapshot mapped = bufferPool("mapped");
        MultiWindowTaskManager taskManager = taskManagerProvider.getIfAvailable();
        int registeredWindows = taskManager == null ? 0 : taskManager.getRegisteredWindowCount();
        int runningWindows = taskManager == null ? 0 : taskManager.getRunningWindowCount();

        log.info("[client-memory] trigger={} windowId={} heapUsedMiB={} heapCommittedMiB={} heapMaxMiB={} "
                        + "nonHeapUsedMiB={} nonHeapCommittedMiB={} directUsedMiB={} directCapacityMiB={} "
                        + "directCount={} mappedUsedMiB={} mappedCapacityMiB={} mappedCount={} liveThreads={} "
                        + "peakThreads={} registeredWindows={} runningWindows={} inputQueue={}",
                normalized(trigger), normalized(windowId),
                mib(heap.getUsed()), mib(heap.getCommitted()), mib(heap.getMax()),
                mib(nonHeap.getUsed()), mib(nonHeap.getCommitted()),
                mib(direct.memoryUsed()), mib(direct.totalCapacity()), direct.count(),
                mib(mapped.memoryUsed()), mib(mapped.totalCapacity()), mapped.count(),
                threadBean.getThreadCount(), threadBean.getPeakThreadCount(),
                registeredWindows, runningWindows, inputActionQueue.size());
    }

    @PreDestroy
    void stop() {
        scheduler.shutdownNow();
    }

    private BufferPoolSnapshot bufferPool(String name) {
        for (BufferPoolMXBean pool : bufferPools) {
            if (name.equalsIgnoreCase(pool.getName())) {
                return new BufferPoolSnapshot(pool.getCount(), pool.getMemoryUsed(), pool.getTotalCapacity());
            }
        }
        return BufferPoolSnapshot.UNAVAILABLE;
    }

    private static String mib(long bytes) {
        if (bytes < 0L) {
            return "-1";
        }
        return String.format(Locale.ROOT, "%.1f", bytes / BYTES_PER_MIB);
    }

    private static String normalized(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private record BufferPoolSnapshot(long count, long memoryUsed, long totalCapacity) {
        private static final BufferPoolSnapshot UNAVAILABLE = new BufferPoolSnapshot(-1L, -1L, -1L);
    }
}
