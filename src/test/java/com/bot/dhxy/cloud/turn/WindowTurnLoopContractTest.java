package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnMapSurveyCommand;
import com.bot.dhxy.cloud.turn.protocol.TurnMapSurveyResult;
import com.bot.dhxy.cloud.turn.protocol.TurnRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnResponse;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskCode;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskQueueFailurePolicy;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskStartAck;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskStartRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskTerminalResult;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowRect;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowTurnLoopContractTest {

    @Test
    void transportOnlyLoopDoesNotRequireCloudTaskTerminalBeforeRemoval() {
        TurnClient client = new TurnClient() {
            @Override
            public TurnExchangeResult exchange(TurnRequest request, byte[] optionalPng) {
                return TurnExchangeResult.accepted(new TurnResponse(
                        TurnResponse.Status.IDLE, null, null, null, null));
            }

            @Override
            public TurnTemplateDownload downloadTemplate(String templateKey, String ifNoneMatch) {
                throw new AssertionError("removal policy does not download templates");
            }
        };
        WindowTurnLoop loop = new WindowTurnLoop(
                "device", "window-a", 1_000L, WindowTurnLoopContractTest::metadata,
                client, action -> {
                    throw new AssertionError("removal policy does not execute actions");
                });

        assertTrue(TurnModeGuard.canRemoveStoppedLoop(loop),
                "map-survey/transport loops own no Cloud task terminal");

        loop.attachStartRequest(new TurnTaskStartRequest(
                "task-run", List.of(TurnTaskCode.XINSHOU), List.of(1),
                TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE));

        assertFalse(TurnModeGuard.canRemoveStoppedLoop(loop),
                "a real task loop remains fenced until Cloud acknowledges its terminal");
    }

    @Test
    void explicitCloudStartRejectionCanBeRemovedButGenericHttpFailureRemainsFenced() throws Exception {
        TurnTaskStartRequest rejectedStart = new TurnTaskStartRequest(
                "start-rejected", List.of(TurnTaskCode.TIANTING), List.of(1),
                TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE);
        WindowTurnLoop rejectedLoop = new WindowTurnLoop(
                "device", "window-rejected", 1_000L, WindowTurnLoopContractTest::metadata,
                (request, optionalPng) -> {
                    throw new TurnTransportException(
                            TurnTransportException.Kind.HTTP_STATUS,
                            "Cloud request rejected [TASK_START_REJECTED]: team missing",
                            409,
                            "TASK_START_REJECTED",
                            null);
                },
                action -> { throw new AssertionError("rejected start must not execute an action"); });
        rejectedLoop.attachStartRequest(rejectedStart);
        rejectedLoop.start();
        assertTrue(rejectedLoop.awaitStopped(Duration.ofSeconds(2)));
        assertTrue(rejectedLoop.wasTaskStartExplicitlyRejected());
        assertTrue(TurnModeGuard.canRemoveStoppedLoop(rejectedLoop),
                "Cloud explicitly denied the start before an ACK, so no RunSlot can remain");

        WindowTurnLoop uncertainLoop = new WindowTurnLoop(
                "device", "window-uncertain", 1_000L, WindowTurnLoopContractTest::metadata,
                (request, optionalPng) -> {
                    throw new TurnTransportException(
                            TurnTransportException.Kind.HTTP_STATUS,
                            "Cloud request rejected [OTHER]: unknown",
                            409,
                            "OTHER",
                            null);
                },
                action -> { throw new AssertionError("uncertain start must not execute an action"); });
        uncertainLoop.attachStartRequest(new TurnTaskStartRequest(
                "start-uncertain", List.of(TurnTaskCode.TIANTING), List.of(1),
                TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE));
        uncertainLoop.start();
        assertTrue(uncertainLoop.awaitStopped(Duration.ofSeconds(2)));
        assertFalse(uncertainLoop.wasTaskStartExplicitlyRejected());
        assertFalse(TurnModeGuard.canRemoveStoppedLoop(uncertainLoop),
                "only the exact Cloud rejection code may release a pre-ACK task loop");
    }

    @Test
    void staleTerminalIsIgnoredAndMatchingSkippedTerminalStopsTheExactRun() throws Exception {
        TurnTaskStartRequest startRequest = new TurnTaskStartRequest(
                "run-current", List.of(TurnTaskCode.XIULUO_V2), List.of(0),
                TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE);
        AtomicInteger calls = new AtomicInteger();
        TurnClient client = new TurnClient() {
            @Override
            public TurnExchangeResult exchange(TurnRequest request, byte[] optionalPng) {
                if (calls.incrementAndGet() == 1) {
                    return TurnExchangeResult.accepted(new TurnResponse(
                            TurnResponse.Status.IDLE, null,
                            new TurnTaskStartAck(startRequest.startRequestId()), null, null,
                            new TurnTaskTerminalResult("run-retired", TurnTaskTerminalResult.Status.STOPPED)));
                }
                return TurnExchangeResult.accepted(new TurnResponse(
                        TurnResponse.Status.IDLE, null, null, null, null,
                        new TurnTaskTerminalResult(startRequest.startRequestId(),
                                TurnTaskTerminalResult.Status.SKIPPED)));
            }

            @Override
            public TurnTemplateDownload downloadTemplate(String templateKey, String ifNoneMatch) {
                throw new AssertionError("terminal lifecycle test does not download templates");
            }
        };
        WindowTurnLoop loop = new WindowTurnLoop(
                "device", "window-a", 1_000L, WindowTurnLoopContractTest::metadata,
                client, action -> {
                    throw new AssertionError("terminal lifecycle test must not execute an action");
                });
        loop.attachStartRequest(startRequest);

        loop.start();
        assertTrue(loop.awaitStartAcknowledged(Duration.ofSeconds(2)));
        TurnTaskTerminalResult terminal = loop.taskTerminalResult().get(2, TimeUnit.SECONDS);

        assertEquals(startRequest.startRequestId(), terminal.startRequestId());
        assertEquals(TurnTaskTerminalResult.Status.SKIPPED, terminal.status());
        assertTrue(loop.awaitStopped(Duration.ofSeconds(2)));
        assertTrue(loop.lastFailure() instanceof IllegalStateException,
                "SKIPPED must become a visible failed terminal instead of remaining running");
        assertEquals(2, calls.get());
    }

    @Test
    void remoteStartIsNotReportedUntilTheMatchingCloudAckArrives() throws Exception {
        CountDownLatch exchangeEntered = new CountDownLatch(1);
        CountDownLatch releaseAck = new CountDownLatch(1);
        TurnTaskStartRequest startRequest = new TurnTaskStartRequest(
                "start-ack-1", List.of(TurnTaskCode.XIULUO_V2), List.of(0),
                TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE);
        TurnClient client = new TurnClient() {
            @Override
            public TurnExchangeResult exchange(TurnRequest request, byte[] optionalPng)
                    throws TurnTransportException {
                exchangeEntered.countDown();
                try {
                    assertTrue(releaseAck.await(2, TimeUnit.SECONDS));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new TurnTransportException(
                            TurnTransportException.Kind.INTERRUPTED, "interrupted", interrupted);
                }
                return TurnExchangeResult.accepted(new TurnResponse(
                        TurnResponse.Status.IDLE, null,
                        new TurnTaskStartAck(startRequest.startRequestId())));
            }

            @Override
            public TurnTemplateDownload downloadTemplate(String templateKey, String ifNoneMatch) {
                throw new AssertionError("start acknowledgement test does not download templates");
            }
        };
        WindowTurnLoop loop = new WindowTurnLoop(
                "device", "window-a", 1_000L, WindowTurnLoopContractTest::metadata,
                client, action -> {
                    throw new AssertionError("start acknowledgement test must not execute an action");
                });
        loop.attachStartRequest(startRequest);

        loop.start();
        assertTrue(exchangeEntered.await(2, TimeUnit.SECONDS));
        assertFalse(loop.awaitStartAcknowledged(Duration.ofMillis(50)),
                "client control must not report success before Cloud acknowledges the start");
        releaseAck.countDown();
        assertTrue(loop.awaitStartAcknowledged(Duration.ofSeconds(2)));

        loop.stop();
        assertTrue(loop.awaitStopped(Duration.ofSeconds(2)));
    }

    @Test
    void loopRetainsPreviousOutcomeUntilExistingTurnAcknowledgesIt() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/cloud/turn/WindowTurnLoop.java"));
        assertTrue(source.contains("previousOutcome"));
        assertTrue(source.contains("previousOutcome = null"));
        assertTrue(source.contains("lastExecutedActionId"));
        assertTrue(source.contains("TurnProtocolValidator.requireValid"));
    }

    @Test
    void mapSurveyCommandSurvivesUncertainTransportAndCompletesOnceBeforeExactAck() throws Exception {
        ScriptedMapSurveyClient client = new ScriptedMapSurveyClient();
        WindowTurnLoop loop = new WindowTurnLoop(
                "device", "window-a", 1_000L, WindowTurnLoopContractTest::metadata,
                client, action -> {
                    throw new AssertionError("MapSurvey contract must not execute an ACTION");
                });
        TurnMapSurveyCommand command = new TurnMapSurveyCommand(
                "survey-1", TurnMapSurveyCommand.Operation.RECORD_CENTER_ANCHOR, "长安");

        loop.start();
        assertTrue(client.firstExchangeEntered.await(2, TimeUnit.SECONDS));
        CompletableFuture<TurnMapSurveyResult> result = loop.attachMapSurveyCommand(command);
        assertThrows(IllegalStateException.class, () -> loop.attachMapSurveyCommand(command));
        client.releaseFirstExchange.countDown();
        assertTrue(loop.awaitStopped(Duration.ofSeconds(2)));
        assertTrue(loop.lastFailure() instanceof TurnTransportException);
        assertEquals(command, client.requests.get(1).mapSurveyCommand());
        assertFalse(result.isDone(), "uncertain transport must retain the UI completion");

        loop.start();
        TurnMapSurveyResult terminal = result.get(2, TimeUnit.SECONDS);
        assertEquals(TurnMapSurveyResult.Status.COMPLETED, terminal.status());
        assertEquals(command, client.requests.get(2).mapSurveyCommand(),
                "restart resends the exact immutable command");
        assertEquals(command, client.requests.get(3).mapSurveyCommand(),
                "ACCEPTED keeps the command pending");
        assertNull(client.requests.get(4).mapSurveyCommand());
        assertEquals(command.commandId(), client.requests.get(4).mapSurveyResultAckId());

        assertTrue(client.ackAccepted.await(2, TimeUnit.SECONDS));
        assertTrue(client.afterAckExchangeEntered.await(2, TimeUnit.SECONDS));
        TurnMapSurveyCommand next = new TurnMapSurveyCommand(
                "survey-2", TurnMapSurveyCommand.Operation.RECORD_LEFT_BOUNDARY, "长安");
        assertFalse(loop.attachMapSurveyCommand(next).isDone(),
                "a successful acknowledgement clears the prior command for the next UI operation");
        loop.stop();
        assertTrue(loop.awaitStopped(Duration.ofSeconds(2)));
    }

    private static TurnWindowMetadata metadata() {
        return new TurnWindowMetadata(
                "device", "window-a", "game", "0x1", 40L,
                new TurnWindowRect(0, 0, 1024, 768), false, false,
                null, "UNKNOWN", null, null, false, false, "NORMAL");
    }

    private static final class ScriptedMapSurveyClient implements TurnClient {
        private final List<TurnRequest> requests = new ArrayList<>();
        private final AtomicInteger calls = new AtomicInteger();
        private final CountDownLatch firstExchangeEntered = new CountDownLatch(1);
        private final CountDownLatch releaseFirstExchange = new CountDownLatch(1);
        private final CountDownLatch ackAccepted = new CountDownLatch(1);
        private final CountDownLatch afterAckExchangeEntered = new CountDownLatch(1);

        @Override
        public synchronized TurnExchangeResult exchange(TurnRequest request, byte[] optionalPng)
                throws TurnTransportException {
            requests.add(request);
            int call = calls.incrementAndGet();
            if (call == 1) {
                firstExchangeEntered.countDown();
                await(releaseFirstExchange);
                return idle(null);
            }
            if (call == 2) {
                throw new TurnTransportException(TurnTransportException.Kind.NETWORK, "uncertain");
            }
            if (call == 3) {
                return idle(new TurnMapSurveyResult(
                        "survey-1", TurnMapSurveyResult.Status.ACCEPTED, "accepted", "长安", null, null));
            }
            if (call == 4) {
                return idle(new TurnMapSurveyResult(
                        "survey-1", TurnMapSurveyResult.Status.COMPLETED, "recorded", "长安", 10, 20));
            }
            if (call == 5) {
                ackAccepted.countDown();
                return idle(null);
            }
            afterAckExchangeEntered.countDown();
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                throw new AssertionError("loop was not stopped");
            } catch (InterruptedException stopped) {
                Thread.currentThread().interrupt();
                throw new TurnTransportException(TurnTransportException.Kind.INTERRUPTED, "stopped", stopped);
            }
        }

        @Override
        public TurnTemplateDownload downloadTemplate(String templateKey, String ifNoneMatch) {
            throw new AssertionError("MapSurvey contract must not download templates");
        }

        private static TurnExchangeResult idle(TurnMapSurveyResult result) {
            return TurnExchangeResult.accepted(new TurnResponse(
                    TurnResponse.Status.IDLE, null, null, null, result));
        }

        private static void await(CountDownLatch latch) throws TurnTransportException {
            try {
                assertTrue(latch.await(2, TimeUnit.SECONDS));
            } catch (InterruptedException stopped) {
                Thread.currentThread().interrupt();
                throw new TurnTransportException(TurnTransportException.Kind.INTERRUPTED, "stopped", stopped);
            }
        }
    }
}
