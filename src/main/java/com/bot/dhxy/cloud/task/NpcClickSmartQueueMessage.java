package com.bot.dhxy.cloud.task;

import lombok.Builder;
import lombok.Value;

import java.awt.Point;
import java.util.List;

@Value
@Builder
public class NpcClickSmartQueueMessage {
    @Builder.Default
    Type type = Type.INVALID;
    String sessionId;
    String windowId;
    String taskRunId;
    String decisionId;
    String strategy;
    Point windowRelativeClickPoint;
    String candidateBox;
    String matchedText;
    @Builder.Default
    List<Point> ctrlProbePoints = List.of();
    String reason;
    @Builder.Default
    double confidence = 0.0d;

    public boolean isOrdinaryClickCandidate() {
        return type == Type.TOOLTIP
                || type == Type.YELLOW_NAME
                || type == Type.PURPLE_FORMULA;
    }

    public boolean hasClickPoint() {
        return windowRelativeClickPoint != null;
    }

    public enum Type {
        MEMORY,
        TOOLTIP,
        YELLOW_NAME,
        PURPLE_FORMULA,
        CTRL_CANDIDATES,
        WAIT,
        END,
        INVALID
    }
}
