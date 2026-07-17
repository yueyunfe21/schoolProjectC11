package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * W-696-NPC-TOOLTIP-PREPAREDPOINT-CONTRACT-COHORT-1: closed DHXY-side wire result for the NPC
 * task-tooltip local macro. Mirrors the Cloud {@code NpcTaskTooltipMacroResult} contract exactly,
 * including the self-verified terminal/payload/clickProduced combination: a verify verdict carries a
 * produced click with a learning payload; the pre-scan/miss terminals carry neither; interruption /
 * mechanics failure carry no payload.
 *
 * <p>Standalone contract type; the sealed transport / kind binding is the deferred shared-integration
 * seam.</p>
 */
@Value
@Jacksonized
public class RemoteNpcTaskTooltipMacroResultPayload {

    Status status;
    boolean clickProduced;
    LearnedPoint payload;
    String reason;

    @Builder
    public RemoteNpcTaskTooltipMacroResultPayload(Status status, boolean clickProduced, LearnedPoint payload, String reason) {
        this.status = requireNonNull(status, "status");
        switch (status) {
            case VERIFIED, CLICK_NOT_VERIFIED ->
                    require(clickProduced && payload != null, status + " requires a produced click with a payload");
            case NOT_FOUND, BINDING_UNAVAILABLE, TEMPLATE_UNAVAILABLE ->
                    require(!clickProduced && payload == null, status + " requires no click and no payload");
            case INTERRUPTED, MECHANICS_FAILED ->
                    require(payload == null, status + " must not carry a payload");
        }
        this.clickProduced = clickProduced;
        this.payload = payload;
        this.reason = reason;
    }

    public enum Status {
        VERIFIED,
        CLICK_NOT_VERIFIED,
        NOT_FOUND,
        BINDING_UNAVAILABLE,
        TEMPLATE_UNAVAILABLE,
        INTERRUPTED,
        MECHANICS_FAILED
    }

    /** Learned click point: record point ({@code tooltipCenterY+90}, screen-absolute) and learned ROI. */
    @Value
    @Jacksonized
    public static class LearnedPoint {
        int recordPointX;
        int recordPointY;
        LearnedRoi learnedRoi;

        @Builder
        public LearnedPoint(int recordPointX, int recordPointY, LearnedRoi learnedRoi) {
            this.recordPointX = recordPointX;
            this.recordPointY = recordPointY;
            this.learnedRoi = requireNonNull(learnedRoi, "learnedRoi");
        }
    }

    /** Window-relative learned ROI (right/bottom edges exclusive). */
    @Value
    @Jacksonized
    public static class LearnedRoi {
        int x1;
        int y1;
        int x2;
        int y2;

        @Builder
        public LearnedRoi(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
