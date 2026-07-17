package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class RemoteInputActionDto {
    RemoteInputActionType type;
    Integer x;
    Integer y;
    Integer endX;
    Integer endY;
    Integer delayMs;
    Integer intervalMs;
    Integer clicks;
    String text;

    void validate() {
        if (type == null) {
            throw new IllegalArgumentException("input action type is required");
        }
        switch (type) {
            case CLICK_LEFT, CLICK_RIGHT -> {
                requireCoordinates();
                requireNonNegative(delayMs, "delayMs");
                forbid("endX", endX, "endY", endY, "intervalMs", intervalMs,
                        "clicks", clicks, "text", text);
            }
            case DOUBLE_RIGHT_CLICK -> {
                requireCoordinates();
                requireNonNegative(delayMs, "delayMs");
                requireNonNegative(intervalMs, "intervalMs");
                forbid("endX", endX, "endY", endY, "clicks", clicks, "text", text);
            }
            case MOVE_MOUSE -> {
                requireCoordinates();
                forbid("endX", endX, "endY", endY, "delayMs", delayMs,
                        "intervalMs", intervalMs, "clicks", clicks, "text", text);
            }
            case DRAG_AND_DROP -> {
                requireCoordinates();
                requireValue(endX, "endX");
                requireValue(endY, "endY");
                forbid("delayMs", delayMs, "intervalMs", intervalMs, "clicks", clicks, "text", text);
            }
            case TYPE_TEXT_UNICODE, PASTE_TEXT -> {
                requireValue(text, "text");
                forbid("x", x, "y", y, "endX", endX, "endY", endY, "delayMs", delayMs,
                        "intervalMs", intervalMs, "clicks", clicks);
            }
            case SCROLL_DOWN, SCROLL_UP -> {
                requireNonNegative(clicks, "clicks");
                forbid("x", x, "y", y, "endX", endX, "endY", endY, "delayMs", delayMs,
                        "intervalMs", intervalMs, "text", text);
            }
            case SLEEP -> {
                requireNonNegative(delayMs, "delayMs");
                forbid("x", x, "y", y, "endX", endX, "endY", endY,
                        "intervalMs", intervalMs, "clicks", clicks, "text", text);
            }
            default -> forbid("x", x, "y", y, "endX", endX, "endY", endY,
                    "delayMs", delayMs, "intervalMs", intervalMs, "clicks", clicks, "text", text);
        }
    }

    private void requireCoordinates() {
        requireValue(x, "x");
        requireValue(y, "y");
    }

    private static void requireNonNegative(Integer value, String field) {
        requireValue(value, field);
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
    }

    private static void requireValue(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private void forbid(Object... fieldValuePairs) {
        for (int i = 0; i < fieldValuePairs.length; i += 2) {
            if (fieldValuePairs[i + 1] != null) {
                throw new IllegalArgumentException(
                        type + " forbids field " + fieldValuePairs[i]);
            }
        }
    }
}
