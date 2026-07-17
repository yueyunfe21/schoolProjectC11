package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** Strict typed EXECUTED result for {@code LOCAL_MACRO / BAG_USE_INCENSE}. */
@Value
@Jacksonized
public class RemoteBagUseIncenseMacroResultPayload implements RemoteLocalMacroResultPayload {
    RemoteLocalMacroKind macroKind;
    State state;

    @Builder
    public RemoteBagUseIncenseMacroResultPayload(
            RemoteLocalMacroKind macroKind,
            State state) {
        if (macroKind != RemoteLocalMacroKind.BAG_USE_INCENSE) {
            throw new IllegalArgumentException("macroKind must be BAG_USE_INCENSE");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        this.macroKind = macroKind;
        this.state = state;
    }

    public enum State {
        USED,
        NOT_FOUND
    }
}
