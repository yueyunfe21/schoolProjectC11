package com.bot.dhxy.cloud.turn.protocol.observation;

/**
 * Small raw saturation fact; no pixels or flight decision cross this boundary.
 * @param stageToken exact per-adjustment/per-input sampling stage, no pipe characters
 * @param meanSaturation raw ROI mean in [0,1], NaN denotes unavailable
 * @param geometryKey actual capture and scaled ROI rectangles; no pipe characters
 */
public record FlightSaturationReading(String stageToken, double meanSaturation, String geometryKey) {
    public static final String INTEREST_KEY = "flight-adjustment-saturation";
    public static final String GUARDED_ALT_C = "ALT_C_FLIGHT_ADJUSTMENT";

    /** Encode the closed three-field value inside ObservationFact; capture time stays on the fact. */
    public String encode() {
        if (stageToken == null || stageToken.isBlank() || stageToken.contains("|")
                || geometryKey == null || geometryKey.isBlank() || geometryKey.contains("|")) {
            throw new IllegalArgumentException("Invalid flight sample identity");
        }
        return stageToken + "|" + meanSaturation + "|" + geometryKey;
    }

    /** Decode the closed value, throwing for malformed input; consumers must not fabricate a sample. */
    public static FlightSaturationReading decode(String value) {
        String[] fields = value == null ? new String[0] : value.split("\\|", -1);
        if (fields.length != 3 || fields[0].isBlank() || fields[2].isBlank()) {
            throw new IllegalArgumentException("Invalid flight sample");
        }
        return new FlightSaturationReading(fields[0], Double.parseDouble(fields[1]), fields[2]);
    }
}
