package com.bot.dhxy.tools;

import org.slf4j.Logger;

/**
 * Small helper for consistent latency logs on high-frequency automation paths.
 *
 * <p>The log format intentionally starts with {@code [latency]} and keeps timing in milliseconds so
 * a later dashboard can parse one stable pattern instead of reverse-engineering many business logs.
 * Callers should only instrument meaningful boundary methods, not every tiny helper.</p>
 */
public final class LatencyMetrics {

    private LatencyMetrics() {
    }

    /** @return monotonic start timestamp from {@link System#nanoTime()}. */
    public static long start() {
        return System.nanoTime();
    }

    /**
     * @param startedNanos value returned by {@link #start()}.
     * @return elapsed wall-clock duration in rounded-up milliseconds.
     */
    public static long elapsedMs(long startedNanos) {
        long elapsedNanos = Math.max(0L, System.nanoTime() - startedNanos);
        return Math.max(0L, (elapsedNanos + 999_999L) / 1_000_000L);
    }

    /**
     * Emit one parseable latency line.
     *
     * @param log owner logger.
     * @param event stable event name, for example {@code npc.click.smart}.
     * @param startedNanos value returned by {@link #start()}.
     * @param detail compact free-form key/value text; use {@code "-"} when there is nothing extra.
     */
    public static void info(Logger log, String event, long startedNanos, String detail) {
        if (log == null) {
            return;
        }
        log.info("[latency] event={} elapsedMs={} detail={}",
                event, elapsedMs(startedNanos), detail == null || detail.isBlank() ? "-" : detail);
    }
}
