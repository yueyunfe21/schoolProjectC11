package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnMetricEventPayload;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.bot.dhxy.metrics.AutomationMetricsService;
import com.bot.dhxy.metrics.DiagnosticCaseCaptureService;
import com.bot.dhxy.model.metrics.AutomationMetricEvent;
import com.bot.dhxy.model.metrics.AutomationMetricEventType;
import com.bot.dhxy.model.metrics.AutomationMetricStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TURN-40B-C1: the metric wire adapter reconstructs one {@code AutomationMetricEvent} from the
 * exact typed payload — all five persisted identity fields and the verbatim {@code caseDir} —
 * mirrors the baseline round/failure attribute composition, and performs exactly one true record
 * through the sole {@code recordWireEvent} seam with the frozen dashboard mapping
 * STARTED=false, FINISHED=true, FAILURE_CASE=false. No input queue, no state, no retry.
 */
class MetricsLocalOperationExecutorContractTest {

    private static final String CASE_DIR =
            "D:\\cloud\\cases\\2026-07-18\\xiuluo_v2-round8-PRE_COMBAT_TIMEOUT";

    @Test
    void roundStartedReconstructsIdentityAndCompositionAndNeverQueuesDashboard() {
        RecordingMetricsService recording = new RecordingMetricsService();
        MetricsLocalOperationExecutor executor = new MetricsLocalOperationExecutor(recording);

        LocalServiceExecution result = executor.execute(call(
                TurnLocalOperation.METRIC_RECORD_ROUND_STARTED,
                new TurnMetricEventPayload(
                        "wubei", "五倍", "window-1", "LEADER", "0x5150",
                        "round-7", 7, "普通怪", null, null, "五倍轮次开始", null,
                        null, null, null, null,
                        Map.of("sourcePhase", "ACCEPT", "source", "hot-start"))));

        assertEquals(TurnStepResult.Status.COMPLETED, result.status());
        assertEquals(1, recording.events.size(), "exactly one true record per wire call");
        assertEquals(List.of(false), recording.dashboardFlags, "round start never queues the dashboard");
        AutomationMetricEvent event = recording.events.get(0);
        assertIdentity(event);
        assertEquals(AutomationMetricEventType.TASK_ROUND_STARTED, event.getEventType());
        assertEquals(AutomationMetricStatus.STARTED, event.getStatus());
        assertEquals("round-7", event.getRunId());
        assertEquals("ACCEPT", event.getPhase(), "sourcePhase attribute drives the phase exactly as baseline");
        assertEquals("五倍轮次开始", event.getMessage());
        assertEquals("round-7", event.getAttributes().get("roundId"));
        assertEquals("7", event.getAttributes().get("roundNumber"));
        assertEquals("普通怪", event.getAttributes().get("roundType"));
        assertEquals("hot-start", event.getAttributes().get("source"));
    }

    @Test
    void roundFinishedReconstructsResultFieldsAndQueuesDashboardExactlyOnce() {
        RecordingMetricsService recording = new RecordingMetricsService();
        MetricsLocalOperationExecutor executor = new MetricsLocalOperationExecutor(recording);

        LocalServiceExecution result = executor.execute(call(
                TurnLocalOperation.METRIC_RECORD_ROUND_FINISHED,
                new TurnMetricEventPayload(
                        "wubei", "五倍", "window-1", "LEADER", "0x5150",
                        "round-7", 7, "普通怪", "FAILED", "FAILED_REACCEPT", "轮次失败", 4321L,
                        null, null, null, null,
                        Map.of("sourcePhase", "COMBAT", "caseId", "case-9"))));

        assertEquals(TurnStepResult.Status.COMPLETED, result.status());
        assertEquals(1, recording.events.size());
        assertEquals(List.of(true), recording.dashboardFlags,
                "round finish queues the dashboard exactly once — the frozen FINISHED=true mapping");
        AutomationMetricEvent event = recording.events.get(0);
        assertIdentity(event);
        assertEquals(AutomationMetricEventType.TASK_ROUND_FINISHED, event.getEventType());
        assertEquals(AutomationMetricStatus.FAILED, event.getStatus());
        assertEquals(Long.valueOf(4321L), event.getElapsedMs());
        assertEquals("FAILED_REACCEPT", event.getErrorCode(),
                "a non-SUCCESS finish keeps its result code as the error code, mirroring baseline");
        assertEquals("case-9", event.getCaseId());
        assertEquals("FAILED_REACCEPT", event.getAttributes().get("resultCode"));
        assertEquals("COMBAT", event.getPhase());
    }

    @Test
    void successfulFinishCarriesNoErrorCodeExactlyAsBaseline() {
        RecordingMetricsService recording = new RecordingMetricsService();
        MetricsLocalOperationExecutor executor = new MetricsLocalOperationExecutor(recording);

        executor.execute(call(
                TurnLocalOperation.METRIC_RECORD_ROUND_FINISHED,
                new TurnMetricEventPayload(
                        "wubei", "五倍", "window-1", "LEADER", "0x5150",
                        "round-8", 8, "普通怪", "SUCCESS", "SUCCESS", "轮次完成", 1000L,
                        null, null, null, null, Map.of())));

        AutomationMetricEvent event = recording.events.get(0);
        assertEquals(AutomationMetricStatus.SUCCESS, event.getStatus());
        assertNull(event.getErrorCode(), "SUCCESS never records an error code");
        assertEquals("SUCCESS", event.getAttributes().get("resultCode"));
    }

    @Test
    void failureCaseKeepsCaseDirVerbatimAndDerivesCaseIdFromItsFileName() {
        RecordingMetricsService recording = new RecordingMetricsService();
        MetricsLocalOperationExecutor executor = new MetricsLocalOperationExecutor(recording);

        LocalServiceExecution result = executor.execute(call(
                TurnLocalOperation.METRIC_RECORD_XIULUO_FAILURE_CASE,
                new TurnMetricEventPayload(
                        "xiuluo_v2", "修罗", "window-2", "MEMBER", "0x5151",
                        null, null, null, null, null, "watchdog timeout", null,
                        CASE_DIR, "PRE_COMBAT_TIMEOUT", "WAIT_TRACKER", 8, null)));

        assertEquals(TurnStepResult.Status.COMPLETED, result.status());
        assertEquals(List.of(false), recording.dashboardFlags, "failure case never queues the dashboard");
        AutomationMetricEvent event = recording.events.get(0);
        assertEquals(AutomationMetricEventType.XIULUO_FAILURE_CASE, event.getEventType());
        assertEquals(AutomationMetricStatus.FAILED, event.getStatus());
        assertEquals("PRE_COMBAT_TIMEOUT", event.getErrorCode());
        assertEquals("WAIT_TRACKER", event.getPhase());
        assertEquals("xiuluo_v2-round8-PRE_COMBAT_TIMEOUT", event.getCaseId(),
                "the case id is the archive directory file name, mirroring baseline");
        assertEquals(CASE_DIR, event.getAttributes().get("caseDir"),
                "the Cloud filesystem locator rides and lands verbatim — never rewritten locally");
        assertEquals("8", event.getAttributes().get("round"));
    }

    @Test
    void unknownFinishStatusFailsClosedWithZeroRecordAndZeroRewrite() {
        RecordingMetricsService recording = new RecordingMetricsService();
        MetricsLocalOperationExecutor executor = new MetricsLocalOperationExecutor(recording);

        LocalServiceExecution result = executor.execute(call(
                TurnLocalOperation.METRIC_RECORD_ROUND_FINISHED,
                new TurnMetricEventPayload(
                        "wubei", "五倍", "window-1", "LEADER", "0x5150",
                        "round-9", 9, "普通怪", "NOT_A_STATUS", "OK", "m", 10L,
                        null, null, null, null, Map.of())));

        assertEquals(TurnStepResult.Status.FAILED, result.status());
        assertEquals("METRIC_STATUS_INVALID", result.code(),
                "the adapter never normalizes an illegal status — it fails closed");
        assertTrue(recording.events.isEmpty(), "an illegal status performs zero records");
    }

    @Test
    void missingPayloadFailsClosedWithoutAnyRecord() {
        RecordingMetricsService recording = new RecordingMetricsService();
        MetricsLocalOperationExecutor executor = new MetricsLocalOperationExecutor(recording);

        LocalServiceExecution result = executor.execute(new TurnLocalServiceCall(
                TurnLocalOperation.METRIC_RECORD_ROUND_STARTED, null, null, null, null, null, null));

        assertEquals(TurnStepResult.Status.FAILED, result.status());
        assertEquals("METRIC_ARGUMENTS_MISSING", result.code());
        assertTrue(recording.events.isEmpty(), "a rejected call performs zero records");
    }

    private static void assertIdentity(AutomationMetricEvent event) {
        assertEquals("wubei", event.getTaskCode());
        assertEquals("五倍", event.getTaskName());
        assertEquals("window-1", event.getWindowId());
        assertEquals("LEADER", event.getWindowRole());
        assertEquals("0x5150", event.getNativeWindowHandle());
    }

    private static TurnLocalServiceCall call(TurnLocalOperation operation, TurnMetricEventPayload payload) {
        return new TurnLocalServiceCall(operation, null, null, null, null, null, payload);
    }

    /** Captures the seam inputs; no super call, so the contract test touches no filesystem. */
    private static final class RecordingMetricsService extends AutomationMetricsService {
        private final List<AutomationMetricEvent> events = new ArrayList<>();
        private final List<Boolean> dashboardFlags = new ArrayList<>();

        private RecordingMetricsService() {
            super((DiagnosticCaseCaptureService) null);
        }

        @Override
        public void recordWireEvent(AutomationMetricEvent event, boolean queueDashboard) {
            events.add(event);
            dashboardFlags.add(queueDashboard);
        }
    }
}
