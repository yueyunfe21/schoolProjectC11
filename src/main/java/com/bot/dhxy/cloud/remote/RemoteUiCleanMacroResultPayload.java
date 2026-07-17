package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Strict typed EXECUTED result for {@code LOCAL_MACRO / UI_CLEAN}. Mirrors the Cloud closed contract:
 * the state is strictly paired with the operation —
 * {@code CLEAN_UP_ALL -> COMPLETED};
 * {@code CLOSE_ALL_GENERIC_WINDOWS -> CLOSED_ANY|NOTHING_CLOSED};
 * {@code CLEAN_LIGHTWEIGHT_INTERRUPTIONS -> HANDLED|NOT_HANDLED};
 * {@code CLOSE_MAP_SEARCH_INPUT_BY_X2 -> CLOSED|NOT_FOUND}.
 */
@Value
@Jacksonized
public class RemoteUiCleanMacroResultPayload implements RemoteLocalMacroResultPayload {
    RemoteLocalMacroKind macroKind;
    RemoteUiCleanMacroCommandPayload.Operation operation;
    State state;

    @Builder
    public RemoteUiCleanMacroResultPayload(
            RemoteLocalMacroKind macroKind,
            RemoteUiCleanMacroCommandPayload.Operation operation,
            State state) {
        if (macroKind != RemoteLocalMacroKind.UI_CLEAN) {
            throw new IllegalArgumentException("macroKind must be UI_CLEAN");
        }
        if (operation == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        boolean paired = switch (operation) {
            case CLEAN_UP_ALL -> state == State.COMPLETED;
            case CLOSE_ALL_GENERIC_WINDOWS -> state == State.CLOSED_ANY || state == State.NOTHING_CLOSED;
            case CLEAN_LIGHTWEIGHT_INTERRUPTIONS -> state == State.HANDLED || state == State.NOT_HANDLED;
            case CLOSE_MAP_SEARCH_INPUT_BY_X2 -> state == State.CLOSED || state == State.NOT_FOUND;
        };
        if (!paired) {
            throw new IllegalArgumentException("UI_CLEAN state " + state + " is not valid for operation " + operation);
        }
        this.macroKind = macroKind;
        this.operation = operation;
        this.state = state;
    }

    public enum State {
        COMPLETED,
        CLOSED_ANY,
        NOTHING_CLOSED,
        HANDLED,
        NOT_HANDLED,
        CLOSED,
        NOT_FOUND
    }
}
