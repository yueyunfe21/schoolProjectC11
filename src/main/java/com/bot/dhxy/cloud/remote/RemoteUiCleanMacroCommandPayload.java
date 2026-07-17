package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Closed wire command for the {@code LOCAL_MACRO / UI_CLEAN} macro. Mirrors the Cloud closed contract:
 * exactly one of four operations. {@code CLEAN_UP_ALL} and {@code CLOSE_ALL_GENERIC_WINDOWS} carry a
 * {@code null} source; {@code CLEAN_LIGHTWEIGHT_INTERRUPTIONS} and {@code CLOSE_MAP_SEARCH_INPUT_BY_X2}
 * carry a non-blank source. Carries no owner/session/queue/retry.
 */
@Value
@Jacksonized
public class RemoteUiCleanMacroCommandPayload implements RemoteLocalMacroCommandPayload {
    RemoteLocalMacroKind macroKind;
    Operation operation;
    String source;

    @Builder
    public RemoteUiCleanMacroCommandPayload(
            RemoteLocalMacroKind macroKind,
            Operation operation,
            String source) {
        if (macroKind != RemoteLocalMacroKind.UI_CLEAN) {
            throw new IllegalArgumentException("macroKind must be UI_CLEAN");
        }
        if (operation == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        switch (operation) {
            case CLEAN_UP_ALL, CLOSE_ALL_GENERIC_WINDOWS -> {
                if (source != null) {
                    throw new IllegalArgumentException(operation + " must not carry a source");
                }
            }
            case CLEAN_LIGHTWEIGHT_INTERRUPTIONS, CLOSE_MAP_SEARCH_INPUT_BY_X2 -> {
                if (source == null || source.isBlank()) {
                    throw new IllegalArgumentException(operation + " requires a non-blank source");
                }
            }
        }
        this.macroKind = macroKind;
        this.operation = operation;
        this.source = source == null ? null : source.trim();
    }

    public enum Operation {
        CLEAN_UP_ALL,
        CLOSE_ALL_GENERIC_WINDOWS,
        CLEAN_LIGHTWEIGHT_INTERRUPTIONS,
        CLOSE_MAP_SEARCH_INPUT_BY_X2
    }
}
