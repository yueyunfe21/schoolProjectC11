package com.bot.dhxy.cloud.turn.protocol;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TurnEnvelopeGoldenJsonTest {

    @Test
    void completedOutcomeRoundTripsWithExactActionAndCaptureFrame() throws IOException {
        TurnOutcome outcome = TurnProtocolGoldenSupport.assertFixtureRoundTrip(
                "outcome-completed.json", TurnOutcome.class);
        TurnProtocolValidator.requireValid(outcome);

        assertEquals(TurnOutcome.Status.COMPLETED, outcome.status());
        assertEquals("action-input-capture-001", outcome.actionId());
        assertNull(outcome.failedStepIndex());
        assertEquals(List.of(TurnStepResult.Status.COMPLETED, TurnStepResult.Status.COMPLETED,
                        TurnStepResult.Status.COMPLETED, TurnStepResult.Status.COMPLETED),
                outcome.stepResults().stream().map(TurnStepResult::status).toList());
        assertEquals(TurnFramePurpose.CAPTURE, outcome.frame().purpose());
        assertEquals("image/png", outcome.frame().contentType());
        assertEquals(TurnProtocolGoldenSupport.SHA_A, outcome.frame().sha256());
        assertEquals(3, outcome.frame().sourceStepIndex());
        assertEquals(outcome.frame().width(), outcome.frame().region().width());
        assertEquals(outcome.frame().height(), outcome.frame().region().height());
    }

    @Test
    void failedOutcomePinsFailedStepNotRunTailAndFailureFrame() throws IOException {
        TurnOutcome outcome = TurnProtocolGoldenSupport.assertFixtureRoundTrip(
                "outcome-failed-with-frame.json", TurnOutcome.class);
        TurnProtocolValidator.requireValid(outcome);

        assertEquals(TurnOutcome.Status.FAILED, outcome.status());
        assertEquals("action-failed-001", outcome.actionId());
        assertEquals(1, outcome.failedStepIndex());
        assertEquals(List.of(TurnStepResult.Status.COMPLETED, TurnStepResult.Status.FAILED,
                        TurnStepResult.Status.NOT_RUN),
                outcome.stepResults().stream().map(TurnStepResult::status).toList());
        assertEquals(TurnFramePurpose.FAILURE_EVIDENCE, outcome.frame().purpose());
        assertEquals(TurnProtocolGoldenSupport.SHA_B, outcome.frame().sha256());
        assertEquals(1, outcome.frame().sourceStepIndex());
    }

    @Test
    void stoppedAndDuplicateOrUncertainRemainDistinctTerminalOutcomes() throws IOException {
        TurnOutcome stopped = TurnProtocolGoldenSupport.assertFixtureRoundTrip(
                "outcome-stopped.json", TurnOutcome.class);
        TurnOutcome uncertain = TurnProtocolGoldenSupport.assertFixtureRoundTrip(
                "outcome-duplicate-or-uncertain.json", TurnOutcome.class);

        TurnProtocolValidator.requireValid(stopped);
        TurnProtocolValidator.requireValid(uncertain);
        assertEquals(TurnOutcome.Status.STOPPED, stopped.status());
        assertEquals("action-stopped-001", stopped.actionId());
        assertEquals(true, stopped.window().stopRequested());
        assertNull(stopped.failedStepIndex());
        assertEquals(TurnOutcome.Status.DUPLICATE_OR_UNCERTAIN, uncertain.status());
        assertEquals("action-uncertain-001", uncertain.actionId());
        assertNull(uncertain.failedStepIndex());
        assertEquals(List.of(), uncertain.stepResults());
    }

    @Test
    void actionAndIdleResponseUnionsValidateAgainstExactWindow() throws IOException {
        TurnAction action = TurnProtocolGoldenSupport.readFixture("action-input-capture.json", TurnAction.class);
        TurnRequest request = new TurnRequest(1, TurnProtocolGoldenSupport.window(false, false), 25_000L, null, null);
        TurnResponse actionResponse = new TurnResponse(TurnResponse.Status.ACTION, action, null);
        TurnResponse idleResponse = new TurnResponse(TurnResponse.Status.IDLE, null, null);

        assertEquals(actionResponse, TurnProtocolValidator.requireValid(actionResponse, request));
        assertEquals(idleResponse, TurnProtocolValidator.requireValid(idleResponse, request));
    }

    @Test
    void queueOwningBagActionResponseValidatesInsideTheEnvelopeUnion() throws IOException {
        TurnBagOperationArguments bag = new TurnBagOperationArguments(
                null, "bag/probe.png", 5, null, "wubei:probe-first-aid");
        TurnAction bagAction = TurnProtocolGoldenSupport.action(
                "envelope-bag-001",
                List.of(TurnProtocolGoldenSupport.localStep(0, new TurnLocalServiceCall(
                        TurnLocalOperation.BAG_FIND_AND_USE_FROM_BACK, bag, null, null, null))));
        TurnRequest request = new TurnRequest(1, TurnProtocolGoldenSupport.window(false, false), 25_000L, null, null);
        TurnResponse bagResponse = new TurnResponse(TurnResponse.Status.ACTION, bagAction, null);

        assertEquals(bagResponse, TurnProtocolValidator.requireValid(bagResponse, request));
        assertEquals(bagAction, TurnProtocolGoldenSupport.roundTrip(bagAction, TurnAction.class));
    }

    @Test
    void metricActionResponseValidatesInsideTheEnvelopeUnion() throws IOException {
        TurnMetricEventPayload payload = new TurnMetricEventPayload(
                "wubei", "五倍", "window-1", "LEADER", "0x5150",
                "round-7", 7, "普通怪", "SUCCESS", "SUCCESS", "轮次完成", 1234L,
                null, null, null, null, null);
        TurnAction metricAction = TurnProtocolGoldenSupport.action(
                "envelope-metric-001",
                List.of(TurnProtocolGoldenSupport.localStep(0, new TurnLocalServiceCall(
                        TurnLocalOperation.METRIC_RECORD_ROUND_FINISHED,
                        null, null, null, null, null, payload))));
        TurnRequest request = new TurnRequest(1, TurnProtocolGoldenSupport.window(false, false), 25_000L, null, null);
        TurnResponse metricResponse = new TurnResponse(TurnResponse.Status.ACTION, metricAction, null);

        assertEquals(metricResponse, TurnProtocolValidator.requireValid(metricResponse, request));
        assertEquals(metricAction, TurnProtocolGoldenSupport.roundTrip(metricAction, TurnAction.class));
    }
}
