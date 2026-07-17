package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * W-BAG-MACRO-DHXY-WIRE-IMP1: closed wire result for the {@code LOCAL_MACRO / BAG_RETURN_ITEM} macro,
 * parsed only from an {@code EXECUTED} envelope. Mirrors the Cloud result contract exactly: a prescan
 * operation yields {@code FOUND(cachePoint)} or {@code NOT_FOUND(null)}; the cached-use operation yields
 * {@code USED(null)} or {@code NOT_USED(null)}. No mechanicalStatus is duplicated here.
 */
@Value
@Jacksonized
public class RemoteBagReturnItemMacroResultPayload implements RemoteLocalMacroResultPayload {
    RemoteLocalMacroKind macroKind;
    RemoteBagReturnItemMacroCommandPayload.Operation operation;
    State state;
    RemoteBagReturnItemMacroCommandPayload.CachePoint cachePoint;

    @Builder
    public RemoteBagReturnItemMacroResultPayload(
            RemoteLocalMacroKind macroKind,
            RemoteBagReturnItemMacroCommandPayload.Operation operation,
            State state,
            RemoteBagReturnItemMacroCommandPayload.CachePoint cachePoint) {
        this.macroKind = requireNonNull(macroKind, "macroKind");
        require(macroKind == RemoteLocalMacroKind.BAG_RETURN_ITEM,
                "macroKind must be BAG_RETURN_ITEM");
        this.operation = requireNonNull(operation, "operation");
        this.state = requireNonNull(state, "state");

        boolean prescan = operation == RemoteBagReturnItemMacroCommandPayload.Operation.PRESCAN_MAIN_BAG_TASK_PAGE
                || operation == RemoteBagReturnItemMacroCommandPayload.Operation.PRESCAN_MAIN_BAG_FROM_BACK;
        if (prescan) {
            require(state == State.FOUND || state == State.NOT_FOUND,
                    "prescan operations must resolve to FOUND or NOT_FOUND");
            if (state == State.FOUND) {
                this.cachePoint = requireNonNull(cachePoint, "cachePoint");
            } else {
                require(cachePoint == null, "NOT_FOUND must not carry a cachePoint");
                this.cachePoint = null;
            }
        } else {
            require(state == State.USED || state == State.NOT_USED,
                    "USE_CACHED operation must resolve to USED or NOT_USED");
            require(cachePoint == null, "cached-use result must not carry a cachePoint");
            this.cachePoint = null;
        }
    }

    public enum State {
        FOUND,
        NOT_FOUND,
        USED,
        NOT_USED
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
