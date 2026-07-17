package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Closed wire command for the {@code LOCAL_MACRO / NAVIGATE_IN_CURRENT_MAP} macro. Mirrors the Cloud
 * {@code NavigateInCurrentMapMacroCommand} and the committed same-path {@code NavigationRequest}
 * field-for-field, so the handler can restore an identical request and drive the existing local
 * navigation Service without losing any caller-visible behavior.
 */
@Value
@Jacksonized
public class RemoteNavigateInCurrentMapMacroCommandPayload implements RemoteLocalMacroCommandPayload {
    RemoteLocalMacroKind macroKind;
    String targetMapName;
    Integer targetX;
    Integer targetY;
    String targetName;
    boolean randomizeMiniMapClickPoint;
    int miniMapClickRandomRadiusPx;
    boolean keepTurnOnCurrentMapPathing;
    int arrivalTolerance;
    String source;
    String freshCurrentMapName;
    Integer freshCurrentX;
    Integer freshCurrentY;
    long freshCurrentLocationAtMs;
    boolean freshCurrentLocationPhaseBound;

    @Builder
    public RemoteNavigateInCurrentMapMacroCommandPayload(
            RemoteLocalMacroKind macroKind,
            String targetMapName,
            Integer targetX,
            Integer targetY,
            String targetName,
            boolean randomizeMiniMapClickPoint,
            int miniMapClickRandomRadiusPx,
            boolean keepTurnOnCurrentMapPathing,
            int arrivalTolerance,
            String source,
            String freshCurrentMapName,
            Integer freshCurrentX,
            Integer freshCurrentY,
            long freshCurrentLocationAtMs,
            boolean freshCurrentLocationPhaseBound) {
        if (macroKind != RemoteLocalMacroKind.NAVIGATE_IN_CURRENT_MAP) {
            throw new IllegalArgumentException("macroKind must be NAVIGATE_IN_CURRENT_MAP");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source must be non-blank");
        }
        this.macroKind = macroKind;
        this.targetMapName = targetMapName;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetName = targetName;
        this.randomizeMiniMapClickPoint = randomizeMiniMapClickPoint;
        this.miniMapClickRandomRadiusPx = miniMapClickRandomRadiusPx;
        this.keepTurnOnCurrentMapPathing = keepTurnOnCurrentMapPathing;
        this.arrivalTolerance = arrivalTolerance;
        this.source = source.trim();
        this.freshCurrentMapName = freshCurrentMapName;
        this.freshCurrentX = freshCurrentX;
        this.freshCurrentY = freshCurrentY;
        this.freshCurrentLocationAtMs = freshCurrentLocationAtMs;
        this.freshCurrentLocationPhaseBound = freshCurrentLocationPhaseBound;
    }
}
