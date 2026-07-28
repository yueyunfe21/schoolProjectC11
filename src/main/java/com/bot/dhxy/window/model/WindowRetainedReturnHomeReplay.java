package com.bot.dhxy.window.model;

import com.bot.dhxy.cloud.turn.protocol.TurnBagOperationArguments;

import java.util.Objects;

/**
 * Exact per-window post-combat return command retained for one local true-exit replay.
 *
 * @param taskCode XIULUO_V2 or WUBEI only
 * @param tokenId immutable identity of this retained/in-flight replay
 * @param lifecycleGeneration local replay lifecycle generation captured when retained
 * @param observationRunId exact acknowledged observation-plane run identity
 * @param businessTaskRunId exact Cloud queue task identity
 * @param arguments original semantic bag macro arguments
 * @param sourceHwnd exact source native window handle
 * @param sourceX source window screen-left in physical pixels
 * @param sourceY source window screen-top in physical pixels
 * @param sourceWidth source window width in physical pixels
 * @param sourceHeight source window height in physical pixels
 * @param state retained replay lifecycle state
 */
public record WindowRetainedReturnHomeReplay(
        String taskCode,
        String tokenId,
        long lifecycleGeneration,
        String observationRunId,
        String businessTaskRunId,
        TurnBagOperationArguments arguments,
        String windowId,
        String sourceHwnd,
        int sourceX,
        int sourceY,
        int sourceWidth,
        int sourceHeight,
        State state) {

    public WindowRetainedReturnHomeReplay {
        Objects.requireNonNull(taskCode, "taskCode");
        Objects.requireNonNull(tokenId, "tokenId");
        if (lifecycleGeneration < 0L) {
            throw new IllegalArgumentException("lifecycleGeneration must be nonnegative");
        }
        Objects.requireNonNull(observationRunId, "observationRunId");
        Objects.requireNonNull(businessTaskRunId, "businessTaskRunId");
        Objects.requireNonNull(arguments, "arguments");
        Objects.requireNonNull(windowId, "windowId");
        Objects.requireNonNull(sourceHwnd, "sourceHwnd");
        Objects.requireNonNull(state, "state");
    }

    public WindowRetainedReturnHomeReplay withState(State value) {
        return new WindowRetainedReturnHomeReplay(
                taskCode, tokenId, lifecycleGeneration,
                observationRunId, businessTaskRunId, arguments, windowId, sourceHwnd,
                sourceX, sourceY, sourceWidth, sourceHeight, value);
    }

    public enum State {
        RETAINED,
        ARMED,
        REPLAYING
    }
}
