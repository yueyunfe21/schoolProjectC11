package com.bot.dhxy.cloud.turn.protocol;

import java.util.List;

public record TurnTaskStartRequest(
        String startRequestId,
        List<TurnTaskCode> taskCodes,
        TurnTaskQueueFailurePolicy failurePolicy) {

    public TurnTaskStartRequest {
        taskCodes = taskCodes == null ? null : List.copyOf(taskCodes);
    }
}
