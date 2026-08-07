package com.bot.dhxy.cloud.turn.protocol.observation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Structured payload carried by {@link ObservationFactType#POSITION_SAMPLE}.
 *
 * @param mapName exact canonical game map name recognized for the bound window
 * @param x logical game-map X coordinate, not a screen pixel
 * @param y logical game-map Y coordinate, not a screen pixel
 */
public record ObservationPositionValue(String mapName, int x, int y) {

    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    public ObservationPositionValue {
        if (mapName == null || mapName.isBlank()) {
            throw new IllegalArgumentException("mapName must be nonblank");
        }
        mapName = mapName.trim();
    }

    /** Encodes this position as the textual payload of an observation fact. */
    public String encode() {
        try {
            return JSON.writeValueAsString(this);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException("position fact encoding failed", failure);
        }
    }

    /** Decodes and validates one POSITION_SAMPLE textual payload. */
    public static ObservationPositionValue decode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("position fact value must be nonblank");
        }
        try {
            return JSON.readValue(value, ObservationPositionValue.class);
        } catch (IOException failure) {
            throw new IllegalArgumentException("position fact value is not valid JSON", failure);
        }
    }
}
