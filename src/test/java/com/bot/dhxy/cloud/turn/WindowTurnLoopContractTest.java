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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void deterministicPreAck4xxStopsWhileUncertainTransportStillRetries() throws Exception {
        TurnTaskStartRequest rejectedStart = new TurnTaskStartRequest(
                "start-rejected", List.of(TurnTaskCode.TIANTING), List.of(1),
                TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE);
        AtomicInteger rejectedCalls = new AtomicInteger();
        WindowTurnLoop rejectedLoop = new WindowTurnLoop(
                "device", "window-rejected", 1_000L, () -> metadata("window-rejected"),
                new TurnClient() {
                    @Override
                    public TurnExchangeResult exchange(TurnRequest request, byte[] optionalPng)
                            throws TurnTransportException {
                        rejectedCalls.incrementAndGet();
                        throw new TurnTransportException(
                                TurnTransportException.Kind.HTTP_STATUS,
                                "Cloud request rejected [TASK_START_REJECTED]: team missing",
                                409,
                                "TASK_START_REJECTED",
                                null);
                    }

                    @Override
                    public TurnTemplateDownload downloadTemplate(String templateKey, String ifNoneMatch) {
                        throw new AssertionError("rejected start must not download templates");
                    }
                },
                action -> { throw new AssertionError("rejected start must not execute an action"); });
        rejectedLoop.attachStartRequest(rejectedStart);
        rejectedLoop.start();
        assertTrue(rejectedLoop.awaitStopped(Duration.ofSeconds(2)));
        assertEquals(1, rejectedCalls.get(), "a deterministic pre-ACK 4xx must never retry the same invalid request");
        assertTrue(rejectedLoop.wasTaskStartExplicitlyRejected());
        assertNotNull(rejectedLoop.lastFailure());
        assertTrue(rejectedLoop.startAcknowledgement().isCompletedExceptionally());

        AtomicInteger uncertainCalls = new AtomicInteger();
        CountDownLatch uncertainRetried = new CountDownLatch(1);
        WindowTurnLoop uncertainLoop = new WindowTurnLoop(
                "device", "window-uncertain", 1_000L, () -> metadata("window-uncertain"),
                new TurnClient() {
                    @Override
                    public TurnExchangeResult exchange(TurnRequest request, byte[] optionalPng)
                            throws TurnTransportException {
                        if (uncertainCalls.incrementAndGet() >= 2) {
                            uncertainRetried.countDown();
                        }
                        throw new TurnTransportException(
                                TurnTransportException.Kind.NETWORK,
                                "connection reset before a response");
                    }

                    @Override
                    public TurnTemplateDownload downloadTemplate(String templateKey, String ifNoneMatch) {
                        throw new AssertionError("uncertain start must not download templates");
                    }
                },
                action -> { throw new AssertionError("uncertain start must not execute an action"); });
        uncertainLoop.attachStartRequest(new TurnTaskStartRequest(
                "start-uncertain", List.of(TurnTaskCode.TIANTING), List.of(1),
                TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE));
        uncertainLoop.start();
        assertTrue(uncertainRetried.await(3, TimeUnit.SECONDS));
        assertTrue(uncertainLoop.isRunning());
        assertFalse(uncertainLoop.wasTaskStartExplicitlyRejected());
        assertNull(uncertainLoop.lastFailure());
        uncertainLoop.stop();
        assertTrue(uncertainLoop.awaitStopped(Duration.ofSeconds(2)));
    }

    @Test
    void localRuntimeFailureRetriesInsideTheSameLoopUntilExplicitStop() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch retried = new CountDownLatch(1);
        TurnClient client = new TurnClient() {
            @Override
            public TurnExchangeResult exchange(TurnRequest request, byte[] optionalPng)
                    throws TurnTransportException {
                if (calls.incrementAndGet() == 1) {
                    throw new IllegalStateException("local test failure");
                }
                retried.countDown();
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                    throw new AssertionError("loop was not stopped");
                } catch (InterruptedException stopped) {
                    Thread.currentThread().interrupt();
                    throw new TurnTransportException(
                            TurnTransportException.Kind.INTERRUPTED, "stopped", stopped);
                }
            }

            @Override
            public TurnTemplateDownload downloadTemplate(String templateKey, String ifNoneMatch) {
                throw new AssertionError("runtime retry test does not download templates");
            }
        };
        WindowTurnLoop loop = new WindowTurnLoop(
                "device", "window-runtime-retry", 1_000L, () -> metadata("window-runtime-retry"),
                client, action -> { throw new AssertionError("runtime retry test does not execute actions"); });

        loop.start();
        assertTrue(retried.await(3, TimeUnit.SECONDS));
        assertTrue(loop.isRunning());
        assertNull(loop.lastFailure());
        loop.stop();
        assertTrue(loop.awaitStopped(Duration.ofSeconds(2)));
    }

    @Test
    void staleTerminalIsIgnoredAndMatchingSkippedTerminalRemainsRecoverable() throws Exception {
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
        assertNull(loop.lastFailure(),
                "SKIPPED closes only the old Cloud run; the control service must replace it without a failed UI");
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
        assertFalse(loop.startAcknowledgement().isDone());
        assertFalse(loop.awaitStartAcknowledged(Duration.ofMillis(50)),
                "client control must not report success before Cloud acknowledges the start");
        releaseAck.countDown();
        assertTrue(loop.awaitStartAcknowledged(Duration.ofSeconds(2)));
        assertEquals(startRequest.startRequestId(),
                loop.startAcknowledgement().get(2, TimeUnit.SECONDS).startRequestId());

        loop.stop();
        assertTrue(loop.awaitStopped(Duration.ofSeconds(2)));
    }

    @Test
    void retryPolicyBacksOffWithBoundedPerWindowJitterAndNeverProducesATerminalDelay() {
        long first = WindowTurnLoop.failureRetryDelayMs("window-a", 1);
        long second = WindowTurnLoop.failureRetryDelayMs("window-a", 2);
        long third = WindowTurnLoop.failureRetryDelayMs("window-a", 3);
        long fourth = WindowTurnLoop.failureRetryDelayMs("window-a", 4);
        long fifth = WindowTurnLoop.failureRetryDelayMs("window-a", 5);
        long longRunning = WindowTurnLoop.failureRetryDelayMs("window-a", 10_000);

        assertTrue(first >= 200L && first <= 300L);
        assertTrue(second >= 400L && second <= 600L);
        assertTrue(third >= 800L && third <= 1_200L);
        assertTrue(fourth >= 1_600L && fourth <= 2_400L);
        assertTrue(fifth >= 3_200L && fifth <= 4_800L);
        assertTrue(longRunning >= 4_000L && longRunning <= 5_000L,
                "even a permanent failure remains a bounded retry rather than becoming terminal");
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
        TurnMapSurveyResult terminal = result.get(5, TimeUnit.SECONDS);
        assertTrue(loop.isRunning(), "uncertain transport must recover inside the same live loop");
        assertNull(loop.lastFailure());
        assertEquals(command, client.requests.get(1).mapSurveyCommand());
        assertEquals(TurnMapSurveyResult.Status.COMPLETED, terminal.status());
        assertEquals(command, client.requests.get(2).mapSurveyCommand(),
                "automatic retry resends the exact immutable command");
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
        return metadata("window-a");
    }

    private static TurnWindowMetadata metadata(String windowId) {
        return new TurnWindowMetadata(
                "device", windowId, "game", "0x1", 40L,
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
