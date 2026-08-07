package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnContinuationRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowRect;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpsTurnClientContractTest {

    @Test
    void rejectsNonTlsRemoteOrigins() {
        assertThrows(IllegalArgumentException.class, () -> new HttpsTurnClient(
                URI.create("http://example.invalid"), "token", Duration.ofSeconds(1),
                Duration.ofSeconds(2), new ObjectMapper()));
    }

    @Test
    void rejectsInvalidContinuationBeforeTransport() {
        HttpsTurnClient client = new HttpsTurnClient(
                URI.create("http://127.0.0.1:1"), "token", Duration.ofSeconds(1),
                Duration.ofSeconds(2), new ObjectMapper());
        TurnWindowMetadata window = new TurnWindowMetadata(
                "device-1", "window-1", "title", "123", 7L,
                new TurnWindowRect(10, 20, 800, 600), false, false);
        TurnContinuationRequest invalid = new TurnContinuationRequest(
                "action-1", 0, TurnContinuationRequest.Kind.FIVERING_INCENSE,
                TurnContinuationRequest.Stage.STATUS_IMAGE, null, null);

        TurnTransportException failure = assertThrows(TurnTransportException.class,
                () -> client.exchange(new TurnRequest(1, window, 0L, null, null, invalid), null));
        assertTrue(failure.getMessage().contains("continuation requires frame"));
    }

    @Test
    void transportRemainsSingleAttemptOnExistingTurnEndpoint() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/cloud/turn/HttpsTurnClient.java"));
        assertTrue(source.contains("/api/v1/client/turn"));
        assertTrue(source.contains("sendOnce(builder.build())"));
        assertTrue(source.contains("TurnMultipartBody.create(requestJson, optionalPng)"));
        assertTrue(!source.contains("retry("));
    }
}
