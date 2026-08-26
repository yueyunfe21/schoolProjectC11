package com.bot.dhxy.cloud.turn.protocol;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class G063StartIngressRoundTripContractTest {

    @Test
    void groupedLeaderAndMemberStartFactsSurviveStrictCloudRoundTrip() throws Exception {
        assertStrictRoundTrip("leader", "LEADER");
        assertStrictRoundTrip("member", "MEMBER");
    }

    private static void assertStrictRoundTrip(String windowId, String role) throws Exception {
        TurnTaskRuntimeSettings settings = new TurnTaskRuntimeSettings(
                false, 1_200_000L, 7_200_000L, 0L,
                false, true, false, false, false, true,
                true, 70, true, 70, true, 70, false, 70,
                TurnTaskRuntimeSettings.LEADER_DEATH_RECOVERY_REACCEPT_TASK);
        TurnWindowMetadata window = new TurnWindowMetadata(
                "dhxy-client", windowId, "Classic Client", "12345", 6789L,
                new TurnWindowRect(0, 0, 1036, 783), false, false, null,
                role, "batch-1", null, true, false, "NORMAL",
                true, "batch-1",
                "anchor-8e335ff8ccf11edbca2ab6b6764cca55cbf62a96ad39f353e6d6fe22a1d29f84",
                null, false);
        TurnRequest request = new TurnRequest(
                1, window, 25_000L, null,
                new TurnTaskStartRequest(
                        "remote-turn-1", List.of(TurnTaskCode.TIANTING), List.of(100),
                        TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE, settings),
                null, null, null);

        byte[] json = new ObjectMapper().writeValueAsBytes(request);
        ObjectMapper strict = new ObjectMapper()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        TurnRequest decoded = strict.readValue(json, TurnRequest.class);

        assertEquals(request, TurnProtocolValidator.requireValid(decoded));
    }
}
