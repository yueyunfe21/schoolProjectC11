package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.protocol.observation.ObservationInterest;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationProtocolValidator;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationRequest;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationResponse;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationRoi;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpsObservationClientRoundTripContractTest {

    private static final String TOKEN = "observation-round-trip-token";

    @Test
    void fetchesFiveXiuluoInterestsThenUploadsFiveProductionSizedRoisOverHttp() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/client/observation", exchange -> {
            try {
                assertEquals("Bearer " + TOKEN, exchange.getRequestHeaders().getFirst("Authorization"));
                ObservationRequest request = mapper.readValue(exchange.getRequestBody(), ObservationRequest.class);
                ObservationProtocolValidator.requireValid(request);
                int requestNumber = requests.incrementAndGet();
                if (requestNumber == 1) {
                    assertEquals(0, request.rois().size());
                } else {
                    assertEquals(5, request.rois().size());
                }
                byte[] body = mapper.writeValueAsBytes(new ObservationResponse(
                        ObservationProtocolValidator.CONTRACT_VERSION,
                        request.observerSeq(),
                        1L,
                        List.of(),
                        xiuluoInterests(),
                        List.of()));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            HttpsObservationClient client = new HttpsObservationClient(
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                    TOKEN, Duration.ofSeconds(2), Duration.ofSeconds(5), mapper);

            ObservationResponse first = client.send(request(1L, 0L, List.of()));
            assertEquals(5, first.interests().size());

            ObservationRequest roiRequest = request(2L, first.interestRevision(), productionSizedRois());
            byte[] roiJson = mapper.writeValueAsBytes(roiRequest);
            assertTrue(roiJson.length > 256 * 1024,
                    "fresh-runtime five-ROI payload must reproduce the retired 256 KiB envelope failure");
            assertTrue(roiJson.length <= HttpsObservationClient.MAX_JSON_BYTES);

            ObservationResponse second = client.send(roiRequest);
            assertEquals(2L, second.acceptedObserverSeq());
            assertEquals(2, requests.get());
        } finally {
            server.stop(0);
        }
    }

    private static ObservationRequest request(long seq, long revision, List<ObservationRoi> rois) {
        return new ObservationRequest(
                ObservationProtocolValidator.CONTRACT_VERSION,
                "tenant-1", "device-1", "window-1", "7280782", "XIULUO_V2", "run-1",
                seq, System.currentTimeMillis(), revision,
                null, null, null, "window-observation-runner", null,
                List.of(), List.of(), rois);
    }

    private static List<ObservationInterest> xiuluoInterests() {
        return List.of(
                new ObservationInterest("combat-flag", 1_000L, null, 974, 630, 51, 20),
                new ObservationInterest("combat-selection", 1_000L, null, 927, 302, 100, 225),
                new ObservationInterest("combat-top", 1_000L, null, 456, 62, 123, 39),
                new ObservationInterest("coordinate-strip", 2_000L, null, 46, 59, 178, 35),
                new ObservationInterest("xiuluo-dialog", 2_000L, null, 250, 312, 529, 208));
    }

    private static List<ObservationRoi> productionSizedRois() {
        return List.of(
                roi("combat-flag", 974, 630, 51, 20, 1_136),
                roi("combat-selection", 927, 302, 100, 225, 20_361),
                roi("combat-top", 456, 62, 123, 39, 6_309),
                roi("coordinate-strip", 46, 59, 178, 35, 8_517),
                roi("xiuluo-dialog", 250, 312, 529, 208, 400_000));
    }

    private static ObservationRoi roi(String key, int left, int top, int width, int height, int pngBytes) {
        byte[] payload = new byte[pngBytes];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i * 31 + 7);
        }
        return new ObservationRoi(key, left, top, width, height, payload);
    }
}
