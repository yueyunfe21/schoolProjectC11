package com.bot.dhxy.metrics;

import com.bot.dhxy.model.metrics.AutomationMetricEvent;
import com.bot.dhxy.model.metrics.AutomationMetricEventType;
import com.bot.dhxy.model.metrics.AutomationMetricStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TURN-40B-C1: retained behavior test for the sole wire seam
 * {@code recordWireEvent(event, queueDashboard)} on the REAL {@code AutomationMetricsService}.
 *
 * <p>Proves the seam performs exactly one true {@code record(event)} with every identity field and
 * the verbatim {@code attributes.caseDir} preserved, and that the FINISHED-only dashboard queue
 * follows the frozen STARTED=false / FINISHED=true / FAILURE_CASE=false mapping. The dashboard
 * queue check follows the in-package source-structure precedent
 * ({@code AutomationMetricsAsyncDashboardWiringTest}) because the queue itself is an async private
 * member; no runtime, thread, or private worker copy is started here.</p>
 */
class AutomationMetricsWireSeamTest {

    @Test
    void seamRecordsExactlyOnceWithIdentityAndCaseDirPreserved(@TempDir Path dir) {
        ObservableService service = new ObservableService(dir);
        AutomationMetricEvent event = AutomationMetricEvent.builder()
                .taskCode("xiuluo_v2")
                .taskName("修罗")
                .windowId("window-2")
                .windowRole("MEMBER")
                .nativeWindowHandle("0x5151")
                .eventType(AutomationMetricEventType.XIULUO_FAILURE_CASE)
                .status(AutomationMetricStatus.FAILED)
                .phase("WAIT_TRACKER")
                .errorCode("PRE_COMBAT_TIMEOUT")
                .caseId("xiuluo_v2-round8-PRE_COMBAT_TIMEOUT")
                .message("watchdog timeout")
                .attributes(Map.of(
                        "round", "8",
                        "caseDir", "D:\\cloud\\cases\\2026-07-18\\xiuluo_v2-round8-PRE_COMBAT_TIMEOUT"))
                .build();

        service.recordWireEvent(event, false);

        assertEquals(1, service.recorded.size(), "the seam performs exactly one true record");
        AutomationMetricEvent recorded = service.recorded.get(0);
        assertEquals("xiuluo_v2", recorded.getTaskCode());
        assertEquals("修罗", recorded.getTaskName());
        assertEquals("window-2", recorded.getWindowId());
        assertEquals("MEMBER", recorded.getWindowRole());
        assertEquals("0x5151", recorded.getNativeWindowHandle());
        assertEquals("D:\\cloud\\cases\\2026-07-18\\xiuluo_v2-round8-PRE_COMBAT_TIMEOUT",
                recorded.getAttributes().get("caseDir"),
                "the Cloud locator survives the seam verbatim — never rewritten to a local path");
        assertEquals("PRE_COMBAT_TIMEOUT", recorded.getErrorCode());
    }

    @Test
    void seamWithDashboardFlagStillRecordsExactlyOnce(@TempDir Path dir) {
        ObservableService service = new ObservableService(dir);
        AutomationMetricEvent event = AutomationMetricEvent.builder()
                .taskCode("wubei").taskName("五倍").windowId("window-1")
                .windowRole("LEADER").nativeWindowHandle("0x5150")
                .eventType(AutomationMetricEventType.TASK_ROUND_FINISHED)
                .status(AutomationMetricStatus.SUCCESS)
                .runId("round-8")
                .elapsedMs(1000L)
                .message("轮次完成")
                .build();

        service.recordWireEvent(event, true);
        service.recordWireEvent(null, true);

        assertEquals(1, service.recorded.size(),
                "the dashboard flag adds a queue, never a second record; a null event is a no-op");
    }

    @Test
    void seamSourceQueuesDashboardOnlyWhenFlaggedAndOnlyAsRoundFinished(@TempDir Path dir) throws Exception {
        // Source-structure gate (in-package precedent): the async private queue cannot be observed
        // without a runtime, so the frozen conditional itself is asserted against the source.
        Path source = Path.of("src", "main", "java", "com", "bot", "dhxy", "metrics",
                "AutomationMetricsService.java");
        String text = Files.readString(source, StandardCharsets.UTF_8);
        int seamStart = text.indexOf("public void recordWireEvent(");
        assertTrue(seamStart > 0, "the sole seam exists");
        String seam = text.substring(seamStart, text.indexOf("\n    }", seamStart));
        assertTrue(seam.contains("record(event);"),
                "the seam performs the one true record through the existing record(event)");
        assertTrue(seam.contains("if (queueDashboard) {"),
                "the dashboard queue is strictly conditional on the frozen flag");
        assertTrue(seam.contains("queueDashboardWrite(\"round-finished\");"),
                "the queued write is exactly the baseline round-finished dashboard write");
        assertEquals(seam.indexOf("queueDashboardWrite"), seam.lastIndexOf("queueDashboardWrite"),
                "the seam queues at most one dashboard write");
        assertEquals(seam.indexOf("record(event);"), seam.lastIndexOf("record(event);"),
                "the seam never double-records");
    }

    /** The real service over temp paths; record() is captured and still fully executed. */
    private static final class ObservableService extends AutomationMetricsService {
        private final List<AutomationMetricEvent> recorded = new ArrayList<>();

        private ObservableService(Path dir) {
            super(dir.resolve("automation-metrics.jsonl"),
                    dir.resolve("automation-dashboard-data.json"),
                    dir.resolve("automation-dashboard.html"));
        }

        @Override
        public void record(AutomationMetricEvent event) {
            recorded.add(event);
            super.record(event);
        }
    }
}
