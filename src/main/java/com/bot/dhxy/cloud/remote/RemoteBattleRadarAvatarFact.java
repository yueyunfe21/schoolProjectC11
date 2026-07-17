package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * W-696-BATTLE-RADAR-DHXY-FACT-1: closed read-only projection of one bound window's 20x20 leader
 * avatar baseline/probe/refresh. Mirrors the Cloud {@code WindowFact.BattleRadarAvatarFact} contract
 * exactly: a closed {@code state} plus optional diagnostic hover-client point and screen-absolute ROI
 * rectangle that must be present as a full group. A mechanics/transport failure is never disguised as
 * {@code UNCHANGED}.
 */
@Value
@Jacksonized
public class RemoteBattleRadarAvatarFact {
    State state;
    Integer hoverClientX;
    Integer hoverClientY;
    Integer roiScreenLeft;
    Integer roiScreenTop;
    Integer roiScreenRight;
    Integer roiScreenBottom;

    @Builder
    public RemoteBattleRadarAvatarFact(
            State state,
            Integer hoverClientX,
            Integer hoverClientY,
            Integer roiScreenLeft,
            Integer roiScreenTop,
            Integer roiScreenRight,
            Integer roiScreenBottom) {
        this.state = requireNonNull(state, "state");
        boolean hasAnyCoordinate = hoverClientX != null
                || hoverClientY != null
                || roiScreenLeft != null
                || roiScreenTop != null
                || roiScreenRight != null
                || roiScreenBottom != null;
        boolean hasAllCoordinates = hoverClientX != null
                && hoverClientY != null
                && roiScreenLeft != null
                && roiScreenTop != null
                && roiScreenRight != null
                && roiScreenBottom != null;
        if (hasAnyCoordinate) {
            require(hasAllCoordinates,
                    "battle-radar avatar fact coordinates must be present as a full group");
            require(roiScreenRight > roiScreenLeft && roiScreenBottom > roiScreenTop,
                    "battle-radar avatar fact ROI rectangle must have positive dimensions");
        }
        this.hoverClientX = hoverClientX;
        this.hoverClientY = hoverClientY;
        this.roiScreenLeft = roiScreenLeft;
        this.roiScreenTop = roiScreenTop;
        this.roiScreenRight = roiScreenRight;
        this.roiScreenBottom = roiScreenBottom;
    }

    public enum State {
        BASELINE_CAPTURED,
        UNCHANGED,
        CHANGED,
        UNAVAILABLE,
        NOT_CONFIGURED,
        MECHANICS_FAILED
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
