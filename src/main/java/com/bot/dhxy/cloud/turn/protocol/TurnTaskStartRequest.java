package com.bot.dhxy.cloud.turn.protocol;

import java.util.List;

public record TurnTaskStartRequest(
        String startRequestId,
        List<TurnTaskCode> taskCodes,
        List<Integer> taskMaxRuns,
        TurnTaskQueueFailurePolicy failurePolicy) {

    public TurnTaskStartRequest {
        taskCodes = taskCodes == null ? null : List.copyOf(taskCodes);
        taskMaxRuns = taskMaxRuns == null ? null : List.copyOf(taskMaxRuns);
    }

    public TurnTaskStartRequest(String startRequestId,
                                List<TurnTaskCode> taskCodes,
                                TurnTaskQueueFailurePolicy failurePolicy) {
        this(startRequestId, taskCodes, null, failurePolicy);
    }
}
