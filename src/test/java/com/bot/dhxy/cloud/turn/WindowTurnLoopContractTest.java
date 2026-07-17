package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnAction;
import com.bot.dhxy.cloud.turn.protocol.TurnOutcome;
import com.bot.dhxy.cloud.turn.protocol.TurnRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnResponse;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowTurnLoopContractTest {

    @Test
    void acceptedIdleClearsPreviousWhileRepeatedActionIdUsesTheCachedExecution() throws Exception {
        TurnContractFixtures.ActionHarness harness = TurnContractFixtures.actionHarness(true, false);
        TurnAction action = TurnContractFixtures.clickAction("action-cache-1");
        ScriptedTurnClient client = new ScriptedTurnClient()
                .thenResponse(actionResponse(action))
                .thenResponse(actionResponse(action))
                .thenResponse(idleResponse())
                .thenBlockUntilInterrupted();
        WindowTurnLoop loop = loop(client, harness.executor());

        try {
            loop.start();
            assertTrue(client.awaitBlocking(Duration.ofSeconds(3)), "loop must reach the fourth long wait");
            assertEquals(1, harness.queue().submissions.size(),
                    "the same actionId must reuse the cached outcome without a second physical input");
            assertEquals(4, client.requests.size());
            assertNoPrevious(client.requests.get(0));
            assertPrevious(client.requests.get(1), action.actionId(), null);
            assertPrevious(client.requests.get(2), action.actionId(), null);
            assertNoPrevious(client.requests.get(3));
        } finally {
            stopAndAssertStopped(loop, client);
        }

        assertNull(loop.lastFailure());
    }

    @Test
    void uncertainOutcomeUploadIsRetainedAcrossExplicitRestartWithoutReexecutingAction() throws Exception {
        TurnContractFixtures.ActionHarness harness = TurnContractFixtures.actionHarness(true, false);
        TurnAction action = TurnContractFixtures.captureUploadClickAction("action-uncertain-1");
        ScriptedTurnClient client = new ScriptedTurnClient()
                .thenResponse(actionResponse(action))
                .thenMutatePngAndFail(new TurnTransportException(
                         TurnTransportException.Kind.NETWORK,
                         "outcome acknowledgement uncertain"))
                .thenResponse(actionResponse(action))
                .thenResponse(idleResponse())
                .thenBlockUntilInterrupted();
        WindowTurnLoop loop = loop(client, harness.executor());

        try {
            loop.start();
            assertTrue(loop.awaitStopped(Duration.ofSeconds(3)), "typed transport failure must stop the loop");
            joinAndAssertNotAlive(
                    client.lastExchangeThread(),
                    "uncertain transport loop worker must finish before explicit restart");
            assertEquals(2, client.requests.size(), "there is no automatic transport or business retry");
            assertEquals(TurnTransportException.Kind.NETWORK,
                    ((TurnTransportException) loop.lastFailure()).kind());

            byte[] originalPng = client.requests.get(1).optionalPng();
            assertPrevious(client.requests.get(1), action.actionId(), originalPng);
            assertDecodableRoiPng(originalPng);
            assertArrayEquals(originalPng, client.pngBeforeMutation());
            assertFalse(Arrays.equals(originalPng, client.pngAfterMutation()),
                    "the uncertain fake must mutate its request-local byte array");
            assertEquals(1, harness.capture().regionCalls);
            assertEquals(0, harness.capture().fullWindowCalls);
            assertEquals(1, harness.queue().submissions.size());

            loop.start();
            assertTrue(client.awaitBlocking(Duration.ofSeconds(3)), "explicit restart must reach the next long wait");

            assertEquals(5, client.requests.size());
            assertPrevious(client.requests.get(2), action.actionId(), originalPng);
            assertPrevious(client.requests.get(3), action.actionId(), originalPng);
            assertNoPrevious(client.requests.get(4));
            assertEquals(1, harness.capture().regionCalls,
                    "a retained actionId must not capture again after uncertain transport");
            assertEquals(0, harness.capture().fullWindowCalls);
            assertEquals(1, harness.queue().submissions.size(),
                    "a retained actionId must not send input again after uncertain transport");
        } finally {
            stopAndAssertStopped(loop, client);
        }

        assertNull(loop.lastFailure());
    }

    private static WindowTurnLoop loop(TurnClient client, LocalTurnActionExecutor executor) {
        return new WindowTurnLoop(
                TurnContractFixtures.DEVICE_ID,
                TurnContractFixtures.WINDOW_ID,
                60_000L,
                () -> TurnContractFixtures.metadata(false),
                client,
                executor);
    }

    private static TurnExchangeResult actionResponse(TurnAction action) {
        return TurnExchangeResult.accepted(new TurnResponse(TurnResponse.Status.ACTION, action));
    }

    private static TurnExchangeResult idleResponse() {
        return TurnExchangeResult.accepted(new TurnResponse(TurnResponse.Status.IDLE, null));
    }

    private static void assertPrevious(RequestRecord record, String actionId, byte[] expectedPng) {
        TurnOutcome previous = record.request().previousOutcome();
        assertNotNull(previous);
        assertEquals(actionId, previous.actionId());
        assertEquals(TurnContractFixtures.DEVICE_ID, previous.window().deviceId());
        assertEquals(TurnContractFixtures.WINDOW_ID, previous.window().windowId());
        if (expectedPng == null) {
            assertNull(record.optionalPng());
        } else {
            assertArrayEquals(expectedPng, record.optionalPng());
        }
    }

    private static void assertNoPrevious(RequestRecord record) {
        assertNull(record.request().previousOutcome());
        assertNull(record.optionalPng(), "a request without previous outcome must not carry raw PNG");
    }

    private static void assertDecodableRoiPng(byte[] png) throws Exception {
        assertNotNull(png);
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(png));
        assertNotNull(decoded, "previous raw frame must be a decodable PNG");
        try {
            assertEquals(2, decoded.getWidth());
            assertEquals(2, decoded.getHeight());
            assertEquals(TurnContractFixtures.ROI_PIXEL, decoded.getRGB(0, 0));
        } finally {
            decoded.flush();
        }
    }

    private static void stopAndAssertStopped(WindowTurnLoop loop,
                                             ScriptedTurnClient client) throws InterruptedException {
        loop.stop();
        boolean stopped = loop.awaitStopped(Duration.ofSeconds(3));
        Thread worker = client.lastExchangeThread();
        joinAndAssertNotAlive(worker, "owned loop worker must not remain alive after cleanup");
        assertTrue(stopped, "owned loop must stop during unconditional cleanup");
        assertFalse(loop.isRunning());
    }

    private static void joinAndAssertNotAlive(Thread worker,
                                              String message) throws InterruptedException {
        if (worker == null) {
            return;
        }
        worker.join(Duration.ofSeconds(3).toMillis());
        assertFalse(worker.isAlive(), message);
    }

    @FunctionalInterface
    private interface ExchangeStep {
        TurnExchangeResult exchange(TurnRequest request, byte[] optionalPng) throws TurnTransportException;
    }

    private record RequestRecord(TurnRequest request, byte[] optionalPng) {
        private RequestRecord {
            optionalPng = optionalPng == null ? null : optionalPng.clone();
        }

        @Override
        public byte[] optionalPng() {
            return optionalPng == null ? null : optionalPng.clone();
        }
    }

    private static final class ScriptedTurnClient implements TurnClient {
        private final List<ExchangeStep> steps = new ArrayList<>();
        private final List<RequestRecord> requests = new CopyOnWriteArrayList<>();
        private final AtomicInteger nextStep = new AtomicInteger();
        private CountDownLatch blockingEntered;
        private volatile byte[] pngBeforeMutation;
        private volatile byte[] pngAfterMutation;
        private volatile Thread lastExchangeThread;

        ScriptedTurnClient thenResponse(TurnExchangeResult result) {
            steps.add((request, optionalPng) -> result);
            return this;
        }

        ScriptedTurnClient thenMutatePngAndFail(TurnTransportException failure) {
            steps.add((request, optionalPng) -> {
                if (optionalPng == null || optionalPng.length == 0) {
                    throw new AssertionError("uncertain outcome upload must carry raw PNG bytes");
                }
                pngBeforeMutation = optionalPng.clone();
                optionalPng[0] ^= 0x7f;
                pngAfterMutation = optionalPng.clone();
                throw failure;
            });
            return this;
        }

        ScriptedTurnClient thenBlockUntilInterrupted() {
            blockingEntered = new CountDownLatch(1);
            steps.add((request, optionalPng) -> {
                blockingEntered.countDown();
                try {
                    new CountDownLatch(1).await();
                    throw new AssertionError("blocking fake unexpectedly released");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new TurnTransportException(
                            TurnTransportException.Kind.INTERRUPTED,
                            "test long wait interrupted",
                            interrupted);
                }
            });
            return this;
        }

        boolean awaitBlocking(Duration timeout) throws InterruptedException {
            return blockingEntered.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        byte[] pngBeforeMutation() {
            return pngBeforeMutation == null ? null : pngBeforeMutation.clone();
        }

        byte[] pngAfterMutation() {
            return pngAfterMutation == null ? null : pngAfterMutation.clone();
        }

        Thread lastExchangeThread() {
            return lastExchangeThread;
        }

        @Override
        public TurnExchangeResult exchange(TurnRequest request, byte[] optionalPng)
                throws TurnTransportException {
            lastExchangeThread = Thread.currentThread();
            requests.add(new RequestRecord(request, optionalPng));
            int index = nextStep.getAndIncrement();
            if (index >= steps.size()) {
                throw new AssertionError("unexpected extra turn exchange " + index);
            }
            return steps.get(index).exchange(request, optionalPng);
        }

        @Override
        public TurnTemplateDownload downloadTemplate(String templateKey, String ifNoneMatch) {
            throw new AssertionError("unexpected template download");
        }
    }
}
