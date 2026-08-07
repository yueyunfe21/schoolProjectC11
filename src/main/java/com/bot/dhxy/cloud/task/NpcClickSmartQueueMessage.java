package com.bot.dhxy.cloud.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Value;

import java.awt.Point;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

@Value
public class NpcClickSmartQueueMessage {
    Type type;
    String sessionId;
    String windowId;
    String taskRunId;
    String decisionId;
    String strategy;
    @JsonSerialize(using = IntegerPointSerializer.class)
    Point windowRelativeClickPoint;
    String candidateBox;
    String matchedText;
    @JsonSerialize(contentUsing = IntegerPointSerializer.class)
    List<Point> ctrlProbePoints;
    String reason;
    double confidence;

    @Builder
    @JsonCreator
    public NpcClickSmartQueueMessage(
            @JsonProperty("type") Type type,
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("windowId") String windowId,
            @JsonProperty("taskRunId") String taskRunId,
            @JsonProperty("decisionId") String decisionId,
            @JsonProperty("strategy") String strategy,
            @JsonProperty("windowRelativeClickPoint")
            @JsonDeserialize(using = IntegerPointDeserializer.class)
            Point windowRelativeClickPoint,
            @JsonProperty("candidateBox") String candidateBox,
            @JsonProperty("matchedText") String matchedText,
            @JsonProperty("ctrlProbePoints")
            @JsonDeserialize(contentUsing = IntegerPointDeserializer.class)
            List<Point> ctrlProbePoints,
            @JsonProperty("reason") String reason,
            @JsonProperty("confidence") double confidence
    ) {
        this.type = type == null ? Type.INVALID : type;
        this.sessionId = sessionId;
        this.windowId = windowId;
        this.taskRunId = taskRunId;
        this.decisionId = decisionId;
        this.strategy = strategy;
        this.windowRelativeClickPoint = windowRelativeClickPoint;
        this.candidateBox = candidateBox;
        this.matchedText = matchedText;
        this.ctrlProbePoints = ctrlProbePoints == null ? List.of() : List.copyOf(ctrlProbePoints);
        this.reason = reason;
        this.confidence = confidence;
    }

    @JsonIgnore
    public boolean isOrdinaryClickCandidate() {
        return type == Type.FIXED_POINT
                || type == Type.TOOLTIP
                || type == Type.YELLOW_NAME
                || type == Type.PURPLE_FORMULA;
    }

    @JsonIgnore
    public boolean hasClickPoint() {
        return windowRelativeClickPoint != null;
    }

    public enum Type {
        MEMORY,
        FIXED_POINT,
        TOOLTIP,
        YELLOW_NAME,
        PURPLE_FORMULA,
        CTRL_CANDIDATES,
        WAIT,
        END,
        INVALID
    }

    public static final class IntegerPointSerializer extends JsonSerializer<Point> {

        @Override
        public void serialize(Point point, JsonGenerator generator, SerializerProvider serializers)
                throws IOException {
            generator.writeStartObject();
            generator.writeNumberField("x", point.x);
            generator.writeNumberField("y", point.y);
            generator.writeEndObject();
        }
    }

    public static final class IntegerPointDeserializer extends JsonDeserializer<Point> {

        @Override
        public Point deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.isExpectedStartObjectToken()) {
                return context.reportInputMismatch(
                        Point.class,
                        "Point must contain exactly two integer fields: x and y"
                );
            }
            Integer x = null;
            Integer y = null;
            int fieldCount = 0;
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (parser.currentToken() != JsonToken.FIELD_NAME) {
                    return context.reportInputMismatch(
                            Point.class,
                            "Point must contain exactly two integer fields: x and y"
                    );
                }
                String field = parser.currentName();
                JsonToken valueToken = parser.nextToken();
                if (valueToken != JsonToken.VALUE_NUMBER_INT) {
                    return context.reportInputMismatch(
                            Point.class,
                            "Point must contain exactly two integer fields: x and y"
                    );
                }
                BigInteger integer = parser.getBigIntegerValue();
                if (integer.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0
                        || integer.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                    return context.reportInputMismatch(
                            Point.class,
                            "Point must contain exactly two integer fields: x and y"
                    );
                }
                int value = integer.intValue();
                if ("x".equals(field) && x == null) {
                    x = value;
                } else if ("y".equals(field) && y == null) {
                    y = value;
                } else {
                    return context.reportInputMismatch(
                            Point.class,
                            "Point must contain exactly two integer fields: x and y"
                    );
                }
                fieldCount++;
            }
            if (fieldCount != 2 || x == null || y == null) {
                return context.reportInputMismatch(
                        Point.class,
                        "Point must contain exactly two integer fields: x and y"
                );
            }
            return new Point(x, y);
        }
    }
}
