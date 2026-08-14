package com.bot.dhxy.cloud.turn.protocol;

import java.util.List;

public record TurnTaskStartRequest(
        String startRequestId,
        List<TurnTaskCode> taskCodes,
        List<Integer> taskMaxRuns,
        List<Integer> taskInitialCompletedRuns,
        TurnTaskQueueFailurePolicy failurePolicy,
        TurnTaskRuntimeSettings runtimeSettings) {

    public TurnTaskStartRequest {
        taskCodes = taskCodes == null ? null : List.copyOf(taskCodes);
        taskMaxRuns = taskMaxRuns == null ? null : List.copyOf(taskMaxRuns);
        taskInitialCompletedRuns = taskInitialCompletedRuns == null ? null : List.copyOf(taskInitialCompletedRuns);
    }

    public TurnTaskStartRequest(String startRequestId,
                                List<TurnTaskCode> taskCodes,
                                List<Integer> taskMaxRuns,
                                TurnTaskQueueFailurePolicy failurePolicy,
                                TurnTaskRuntimeSettings runtimeSettings) {
        this(startRequestId, taskCodes, taskMaxRuns, zeroInitialCompletedRuns(taskCodes),
                failurePolicy, runtimeSettings);
    }

    public TurnTaskStartRequest(String startRequestId,
                                List<TurnTaskCode> taskCodes,
                                List<Integer> taskMaxRuns,
                                TurnTaskQueueFailurePolicy failurePolicy) {
        this(startRequestId, taskCodes, taskMaxRuns, zeroInitialCompletedRuns(taskCodes),
                failurePolicy, TurnTaskRuntimeSettings.defaults());
    }

    public TurnTaskStartRequest(String startRequestId,
                                List<TurnTaskCode> taskCodes,
                                TurnTaskQueueFailurePolicy failurePolicy) {
        this(startRequestId, taskCodes, null, zeroInitialCompletedRuns(taskCodes),
                failurePolicy, TurnTaskRuntimeSettings.defaults());
    }

    private static List<Integer> zeroInitialCompletedRuns(List<TurnTaskCode> taskCodes) {
        return taskCodes == null ? null : taskCodes.stream().map(ignored -> 0).toList();
    }
}
