package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Strict typed EXECUTED result for {@code LOCAL_MACRO / NAVIGATE_IN_CURRENT_MAP}. Mirrors the Cloud
 * {@code NavigateInCurrentMapMacroResult}: the committed terminal classification only. The human
 * diagnostic reason travels in the common outcome message, so the wire shape matches the other macros.
 */
@Value
@Jacksonized
public class RemoteNavigateInCurrentMapMacroResultPayload implements RemoteLocalMacroResultPayload {
    RemoteLocalMacroKind macroKind;
    State state;

    @Builder
    public RemoteNavigateInCurrentMapMacroResultPayload(
            RemoteLocalMacroKind macroKind,
            State state) {
        if (macroKind != RemoteLocalMacroKind.NAVIGATE_IN_CURRENT_MAP) {
            throw new IllegalArgumentException("macroKind must be NAVIGATE_IN_CURRENT_MAP");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        this.macroKind = macroKind;
        this.state = state;
    }

    /** Mirror of the committed {@code NavigationResultStatus} closed enum, value-for-value. */
    public enum State {
        ARRIVED,
        PATHING_STARTED,
        SUCCESS,
        FAILED,
        STOPPED,
        INTERRUPTED,
        DIALOG_PREPARING,
        MAP_NOT_REACHED,
        POINT_NOT_REACHED,
        DIALOG_OPENED
    }
}
