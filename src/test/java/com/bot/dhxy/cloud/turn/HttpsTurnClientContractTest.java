package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnAction;
import com.bot.dhxy.cloud.turn.protocol.TurnFrameMetadata;
import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnOutcome;
import com.bot.dhxy.cloud.turn.protocol.TurnRegion;
import com.bot.dhxy.cloud.turn.protocol.TurnRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnResponse;
import com.bot.dhxy.cloud.turn.protocol.TurnStep;
import com.bot.dhxy.cloud.turn.protocol.TurnStepType;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowRect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpsTurnClientContractTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsOneJsonRequestAndParsesTheCloudAction() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        AtomicReference<CapturedRequest> captured = new AtomicReference<>();
        TurnAction action = new TurnAction(
                1,
                "action-001",
                "device-1",
                "window-7",
                List.of(new TurnStep(0, TurnStepType.WAIT, null, null, 25L, null, null, null)),
                false);
        URI baseUri = start(exchange -> {
            requests.incrementAndGet();
            captured.set(CapturedRequest.read(exchange));
            respondJson(exchange, 200, new TurnResponse(TurnResponse.Status.ACTION, action));
        });

        TurnExchangeResult result = client(baseUri).exchange(request(null), null);

        assertEquals(1, requests.get(), "turn transport must send exactly once");
        assertEquals(TurnExchangeResult.PreviousOutcomeStatus.ACCEPTED, result.previousOutcomeStatus());
        assertEquals(TurnResponse.Status.ACTION, result.response().status());
        assertEquals("action-001", result.response().action().actionId());
        assertEquals("/api/v1/client/turn", captured.get().path());
        assertEquals("Bearer contract-token", captured.get().authorization());
        assertEquals("application/json", captured.get().contentType());
        TurnRequest decoded = OBJECT_MAPPER.readValue(captured.get().body(), TurnRequest.class);
        assertEquals("window-7", decoded.window().windowId());
        assertEquals(137, decoded.window().windowRect().left());
        assertEquals(241, decoded.window().windowRect().top());
    }

    @Test
    void sendsRawPngInTheSameMultipartRequestWithoutBase64() throws Exception {
        byte[] png = fixturePng();
        AtomicInteger requests = new AtomicInteger();
        AtomicReference<CapturedRequest> captured = new AtomicReference<>();
        URI baseUri = start(exchange -> {
            requests.incrementAndGet();
            captured.set(CapturedRequest.read(exchange));
            respondJson(exchange, 200, new TurnResponse(TurnResponse.Status.IDLE, null));
        });

        TurnExchangeResult result = client(baseUri).exchange(request(completedOutcomeWithFrame(png)), png);

        assertEquals(TurnResponse.Status.IDLE, result.response().status());
        assertEquals(1, requests.get(), "multipart upload must not be retried");
        assertTrue(captured.get().contentType().startsWith("multipart/form-data; boundary="));
        assertTrue(indexOf(captured.get().body(), png) >= 0, "multipart body must contain the exact raw PNG bytes");
        byte[] encoded = Base64.getEncoder().encode(png);
        assertFalse(indexOf(captured.get().body(), encoded) >= 0, "PNG must not be Base64 encoded in JSON");
        String metadataText = new String(captured.get().body(), StandardCharsets.ISO_8859_1);
        assertTrue(metadataText.contains("name=\"metadata\""));
        assertTrue(metadataText.contains("\"actionId\":\"action-before\""));
        assertTrue(metadataText.contains("name=\"frame\"; filename=\"frame.png\""));
        assertArrayEquals(png, slice(captured.get().body(), indexOf(captured.get().body(), png), png.length));
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 409, 503})
    void mapsHttpFailuresWithoutRetry(int status) throws Exception {
        AtomicInteger requests = new AtomicInteger();
        URI baseUri = start(exchange -> {
            requests.incrementAndGet();
            byte[] body = ("status-" + status).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        TurnTransportException failure = assertThrows(
                TurnTransportException.class,
                () -> client(baseUri).exchange(request(null), null));

        assertEquals(TurnTransportException.Kind.HTTP_STATUS, failure.kind());
        assertEquals(status, failure.httpStatus());
        assertEquals(1, requests.get(), "HTTP failures must not trigger an implicit retry");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("strictMalformedResponses")
    void rejectsMalformedSuccessfulResponsesWithoutRetry(String scenario, String responseJson) throws Exception {
        AtomicInteger requests = new AtomicInteger();
        URI baseUri = start(exchange -> {
            requests.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            respondRawJson(exchange, 200, responseJson);
        });

        TurnTransportException failure = assertThrows(
                TurnTransportException.class,
                () -> client(baseUri).exchange(request(null), null),
                scenario);

        assertEquals(TurnTransportException.Kind.RESPONSE_PARSE, failure.kind(), scenario);
        assertEquals(1, requests.get(), scenario + " must not trigger a retry or second POST");
    }

    @Test
    void mapsAnUncertainConnectionCloseWithoutRetry() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        URI baseUri = start(exchange -> {
            requests.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            exchange.close();
        });

        TurnTransportException failure = assertThrows(
                TurnTransportException.class,
                () -> client(baseUri).exchange(request(null), null));

        assertEquals(TurnTransportException.Kind.NETWORK, failure.kind());
        assertTrue(failure.getMessage().contains("acknowledgement is unknown"));
        assertEquals(1, requests.get(), "network uncertainty must remain a single POST attempt");
    }

    @Test
    void interruptedSendIsTypedPreservesInterruptAndNeverIssuesASecondRequest() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        CountDownLatch requestReceived = new CountDownLatch(1);
        CountDownLatch releaseResponse = new CountDownLatch(1);
        URI baseUri = start(exchange -> {
            requests.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            requestReceived.countDown();
            if (!releaseResponse.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("test server response gate timed out");
            }
            respondJson(exchange, 200, new TurnResponse(TurnResponse.Status.IDLE, null));
        });
        AtomicReference<Throwable> observed = new AtomicReference<>();
        AtomicBoolean interruptPreserved = new AtomicBoolean();
        CountDownLatch clientFinished = new CountDownLatch(1);
        Thread clientThread = new Thread(() -> {
            try {
                client(baseUri).exchange(request(null), null);
                observed.set(new AssertionError("interrupted exchange unexpectedly completed"));
            } catch (Throwable failure) {
                observed.set(failure);
            } finally {
                interruptPreserved.set(Thread.currentThread().isInterrupted());
                clientFinished.countDown();
            }
        }, "turn-client-interrupt-contract");

        clientThread.start();
        try {
            assertTrue(requestReceived.await(3, TimeUnit.SECONDS), "the one POST must reach loopback first");
            clientThread.interrupt();
            assertTrue(clientFinished.await(3, TimeUnit.SECONDS), "interrupted HTTP send must terminate promptly");
        } finally {
            releaseResponse.countDown();
            clientThread.join(3_000L);
        }

        TurnTransportException failure = assertInstanceOf(TurnTransportException.class, observed.get());
        assertEquals(TurnTransportException.Kind.INTERRUPTED, failure.kind());
        assertTrue(interruptPreserved.get(), "transport must restore the isolated client's interrupt flag");
        assertEquals(1, requests.get(), "interrupt uncertainty must not trigger a retry or second POST");
        assertFalse(clientThread.isAlive());
    }

    private URI start(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/client/turn", exchange -> {
            try {
                handler.handle(exchange);
            } catch (Throwable failure) {
                exchange.close();
            }
        });
        server.start();
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    private HttpsTurnClient client(URI baseUri) {
        return new HttpsTurnClient(
                baseUri,
                "contract-token",
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                OBJECT_MAPPER);
    }

    private static TurnRequest request(TurnOutcome previousOutcome) {
        return new TurnRequest(1, metadata(false), 1_000L, previousOutcome);
    }

    private static TurnWindowMetadata metadata(boolean stopRequested) {
        return new TurnWindowMetadata(
                "device-1",
                "window-7",
                "game-window-7",
                "12345",
                88L,
                new TurnWindowRect(137, 241, 800, 600),
                stopRequested);
    }

    private static TurnOutcome completedOutcomeWithFrame(byte[] png) throws Exception {
        TurnFrameMetadata frame = new TurnFrameMetadata(
                TurnFramePurpose.CAPTURE,
                "image/png",
                sha256(png),
                2,
                2,
                new TurnRegion(151, 263, 2, 2),
                0);
        return new TurnOutcome(
                1,
                "action-before",
                metadata(false),
                TurnOutcome.Status.COMPLETED,
                null,
                "OK",
                null,
                List.of(),
                frame);
    }

    private static Stream<Arguments> strictMalformedResponses() {
        String actionResponse = """
                {
                  "status": "ACTION",
                  "action": {
                    "contractVersion": %s,
                    "actionId": "action-malformed",
                    "deviceId": "device-1",
                    "windowId": "window-7",
                    "steps": [{
                      "index": 0,
                      "type": "WAIT",
                      "inputAction": null,
                      "input": null,
                      "waitMs": 25,
                      "capture": null,
                      "match": null,
                      "localService": null
                    }],
                    "fullWindowFailureEvidence": false
                  },
                  "taskStartAck": null
                }
                """;
        return Stream.of(
                Arguments.of(
                        "numeric enum is rejected",
                        "{\"status\":0,\"action\":null,\"taskStartAck\":null}"),
                Arguments.of("null primitive is rejected", actionResponse.formatted("null")),
                Arguments.of("quoted numeric coercion is rejected", actionResponse.formatted("\"1\"")),
                Arguments.of("float numeric coercion is rejected", actionResponse.formatted("1.0")));
    }

    private static void respondJson(HttpExchange exchange, int status, Object value) throws IOException {
        byte[] body = OBJECT_MAPPER.writeValueAsBytes(value);
        respondRawJson(exchange, status, new String(body, StandardCharsets.UTF_8));
    }

    private static void respondRawJson(HttpExchange exchange, int status, String value) throws IOException {
        byte[] body = value.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static byte[] fixturePng() throws IOException {
        try (var input = HttpsTurnClientContractTest.class.getResourceAsStream("/cloud-turn/v1/frame-2x2.png")) {
            assertNotNull(input, "frame-2x2.png fixture");
            return input.readAllBytes();
        }
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int index = 0; index <= haystack.length - needle.length; index++) {
            for (int offset = 0; offset < needle.length; offset++) {
                if (haystack[index + offset] != needle[offset]) {
                    continue outer;
                }
            }
            return index;
        }
        return -1;
    }

    private static byte[] slice(byte[] bytes, int start, int length) {
        byte[] copy = new byte[length];
        System.arraycopy(bytes, start, copy, 0, length);
        return copy;
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws Exception;
    }

    private record CapturedRequest(String path, String authorization, String contentType, byte[] body) {
        private static CapturedRequest read(HttpExchange exchange) throws IOException {
            return new CapturedRequest(
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    exchange.getRequestBody().readAllBytes());
        }
    }
}
