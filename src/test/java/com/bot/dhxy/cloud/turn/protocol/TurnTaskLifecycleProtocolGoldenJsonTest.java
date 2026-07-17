package com.bot.dhxy.cloud.turn.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnTaskLifecycleProtocolGoldenJsonTest {

    @Test
    void startRequestAndIdleAckFixturesRoundTripWithOrderedQueueAndExactCorrelation() throws IOException {
        TurnRequest request = TurnProtocolGoldenSupport.assertFixtureRoundTrip("request-start.json", TurnRequest.class);
        TurnResponse response = TurnProtocolGoldenSupport.assertFixtureRoundTrip(
                "response-start-ack-idle.json", TurnResponse.class);

        TurnProtocolValidator.requireValid(request);
        TurnProtocolValidator.requireValid(response, request);
        assertEquals("start-request-001", request.taskStartRequest().startRequestId());
        assertEquals(List.of(TurnTaskCode.WUHUAN_V2, TurnTaskCode.WUBEI, TurnTaskCode.XIULUO_V2,
                TurnTaskCode.AUTO_BATTLE), request.taskStartRequest().taskCodes());
        assertEquals(TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE,
                request.taskStartRequest().failurePolicy());
        assertEquals(TurnResponse.Status.IDLE, response.status());
        assertEquals(request.taskStartRequest().startRequestId(), response.taskStartAck().startRequestId());

        TurnRequest transportRedelivery = TurnProtocolGoldenSupport.roundTrip(request, TurnRequest.class);
        assertEquals(request.taskStartRequest(), transportRedelivery.taskStartRequest());
        assertEquals("start-request-001", transportRedelivery.taskStartRequest().startRequestId());
    }

    @Test
    void orderedQueueIsDefensivelyCopiedAndBothFailurePoliciesRemainClosed() throws IOException {
        List<TurnTaskCode> mutableCodes = new ArrayList<>(List.of(TurnTaskCode.WUBEI, TurnTaskCode.XIULUO_V2));
        TurnTaskStartRequest start = new TurnTaskStartRequest(
                "stable-start-002", mutableCodes, TurnTaskQueueFailurePolicy.STOP_ON_FAILURE);
        mutableCodes.clear();

        assertEquals(List.of(TurnTaskCode.WUBEI, TurnTaskCode.XIULUO_V2), start.taskCodes());
        assertThrows(UnsupportedOperationException.class, () -> start.taskCodes().add(TurnTaskCode.AUTO_BATTLE));
        TurnRequest request = new TurnRequest(
                1, TurnProtocolGoldenSupport.window(false, false), 25_000L, null, start);
        TurnRequest roundTripped = TurnProtocolGoldenSupport.roundTrip(request, TurnRequest.class);
        TurnProtocolValidator.requireValid(roundTripped);
        assertEquals(TurnTaskQueueFailurePolicy.STOP_ON_FAILURE,
                roundTripped.taskStartRequest().failurePolicy());
        assertEquals(List.of(TurnTaskCode.WUBEI, TurnTaskCode.XIULUO_V2),
                roundTripped.taskStartRequest().taskCodes());

        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(new TurnRequest(1,
                request.window(), 25_000L, null,
                new TurnTaskStartRequest("start-empty", List.of(),
                        TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE))));
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(new TurnRequest(1,
                request.window(), 25_000L, null,
                new TurnTaskStartRequest(" ", List.of(TurnTaskCode.WUBEI),
                        TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE))));
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(new TurnRequest(1,
                request.window(), 25_000L, null,
                new TurnTaskStartRequest("start-no-policy", List.of(TurnTaskCode.WUBEI), null))));
    }

    @Test
    void missingMismatchedOrUnsolicitedStartAckFailsClosed() throws IOException {
        TurnRequest request = TurnProtocolGoldenSupport.readFixture("request-start.json", TurnRequest.class);
        TurnResponse exact = TurnProtocolGoldenSupport.readFixture(
                "response-start-ack-idle.json", TurnResponse.class);
        TurnProtocolValidator.requireValid(exact, request);

        TurnResponse missing = new TurnResponse(TurnResponse.Status.IDLE, null, null);
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(missing, request));

        TurnResponse mismatch = new TurnResponse(
                TurnResponse.Status.IDLE, null, new TurnTaskStartAck("different-start-request"));
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(mismatch, request));

        TurnRequest noStart = new TurnRequest(1, request.window(), request.waitTimeoutMs(), null, null);
        assertThrows(IllegalArgumentException.class, () -> TurnProtocolValidator.requireValid(exact, noStart));
    }

    @Test
    void pauseAndStopMetadataRemainIndependentAcrossRoundTrip() throws IOException {
        TurnRequest pause = new TurnRequest(
                1, TurnProtocolGoldenSupport.window(true, false), 25_000L, null, null);
        TurnRequest stop = new TurnRequest(
                1, TurnProtocolGoldenSupport.window(false, true), 25_000L, null, null);

        TurnRequest pauseRoundTrip = TurnProtocolGoldenSupport.roundTrip(pause, TurnRequest.class);
        TurnRequest stopRoundTrip = TurnProtocolGoldenSupport.roundTrip(stop, TurnRequest.class);
        TurnProtocolValidator.requireValid(pauseRoundTrip);
        TurnProtocolValidator.requireValid(stopRoundTrip);
        assertTrue(pauseRoundTrip.window().pauseRequested());
        assertFalse(pauseRoundTrip.window().stopRequested());
        assertFalse(stopRoundTrip.window().pauseRequested());
        assertTrue(stopRoundTrip.window().stopRequested());
        assertEquals(120, pauseRoundTrip.window().windowRect().left());
        assertEquals(80, pauseRoundTrip.window().windowRect().top());
    }

    @Test
    void sleepComputerAndUnknownFailurePolicyAreRejectedByStrictContractMapper() throws IOException {
        for (String forbidden : List.of("SLEEP_COMPUTER", "UNKNOWN_TASK")) {
            ObjectNode taskRequest = (ObjectNode) TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readTree(
                    TurnProtocolGoldenSupport.fixtureBytes("request-start.json"));
            taskRequest.with("taskStartRequest").withArray("taskCodes").set(0,
                    TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.getNodeFactory().textNode(forbidden));
            assertThrows(JsonProcessingException.class, () -> TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                    TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(taskRequest), TurnRequest.class));
        }

        ObjectNode policyRequest = (ObjectNode) TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readTree(
                TurnProtocolGoldenSupport.fixtureBytes("request-start.json"));
        policyRequest.with("taskStartRequest").put("failurePolicy", "BEST_EFFORT");
        assertThrows(JsonProcessingException.class, () -> TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.readValue(
                TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER.writeValueAsBytes(policyRequest), TurnRequest.class));
    }
}
