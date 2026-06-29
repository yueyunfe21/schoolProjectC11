package com.bot.dhxy.metrics;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR129 round-finish dashboard write isolation.
 */
public class AutomationDashboardAsyncWriteWiringTest {

    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/bot/dhxy/metrics/AutomationMetricsService.java"),
                StandardCharsets.UTF_8);

        roundFinishOnlyQueuesDashboardWrite(source);
        dashboardWriterIsBoundedAndBackgroundOwned(source);
        coalescedRequestsAreCountedInFlushLog(source);
        manualWriteRemainsSynchronous(source);
    }

    private static void roundFinishOnlyQueuesDashboardWrite(String source) {
        String method = between(source,
                "public void recordRoundFinished(",
                "public void recordTransaction(");
        require(method.contains("record(baseEvent(context)"),
                "round finish must keep synchronous in-memory/event recording");
        require(method.contains("queueDashboardWrite(\"round-finished\")"),
                "round finish must enqueue dashboard persistence");
        require(!method.contains("writeDashboard()"),
                "round finish must not write dashboard on caller thread");
        require(!method.contains("writeDashboardNow()"),
                "round finish must not call manual synchronous dashboard write");
    }

    private static void dashboardWriterIsBoundedAndBackgroundOwned(String source) {
        require(source.contains("new LinkedBlockingQueue<>(1)"),
                "dashboard write queue must stay bounded to one pending write");
        require(source.contains("dashboardWriterThread = new Thread(this::dashboardWriterLoop"),
                "AutomationMetricsService must own the background dashboard writer");
        require(source.contains("queueDashboardWrite(\"throttled\")"),
                "throttled metric writes must enqueue, not write synchronously");
    }

    private static void coalescedRequestsAreCountedInFlushLog(String source) {
        String queueMethod = between(source,
                "private void queueDashboardWrite(String reason)",
                "private void dashboardWriterLoop()");
        String writerLoop = between(source,
                "private void dashboardWriterLoop()",
                "private synchronized void writeDashboard()");

        require(source.contains("AtomicLong coalescedDashboardWriteRequests"),
                "coalesced dashboard requests must be counted across failed queue offers");
        require(queueMethod.contains("coalescedDashboardWriteRequests.incrementAndGet()"),
                "queue coalescing must increment the shared coalesced counter");
        require(writerLoop.contains("coalescedDashboardWriteRequests.getAndSet(0L)"),
                "flush log must consume the shared coalesced counter");
        require(writerLoop.contains("coalescedRequests={}"),
                "flush log must expose coalesced request count for runtime validation");
    }

    private static void manualWriteRemainsSynchronous(String source) {
        String method = between(source,
                "public Path writeDashboardNow()",
                "public Path getDashboardPath()");
        require(method.contains("writeDashboard();"),
                "manual writeDashboardNow must keep the synchronous dashboard write");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex + start.length());
        if (endIndex < 0) {
            throw new AssertionError("Missing source marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
