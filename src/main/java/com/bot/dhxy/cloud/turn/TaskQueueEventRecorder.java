package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnTaskQueueEvent;

/** Client-side sink for Cloud queue diagnostics; intentionally has no control or input authority. */
@FunctionalInterface
public interface TaskQueueEventRecorder {
    TaskQueueEventRecorder NO_OP = (windowId, event) -> { };

    void record(String windowId, TurnTaskQueueEvent event);
}
