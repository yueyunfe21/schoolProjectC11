package com.bot.dhxy.cloud.turn.protocol;

public record TurnLocalServiceCall(
        TurnLocalOperation operation,
        TurnBagOperationArguments bag,
        TurnUiOperationArguments ui,
        TurnGiveItemOperationArguments giveItem,
        TurnQuestOperationArguments quest,
        TurnWholeTaskRuntimeArguments wholeTaskRuntime,
        TurnMetricEventPayload metric,
        TurnTaskTrackerOperationArguments taskTracker) {

    public TurnLocalServiceCall(
            TurnLocalOperation operation,
            TurnBagOperationArguments bag,
            TurnUiOperationArguments ui,
            TurnGiveItemOperationArguments giveItem,
            TurnQuestOperationArguments quest,
            TurnWholeTaskRuntimeArguments wholeTaskRuntime,
            TurnMetricEventPayload metric) {
        this(operation, bag, ui, giveItem, quest, wholeTaskRuntime, metric, null);
    }

    public TurnLocalServiceCall(
            TurnLocalOperation operation,
            TurnBagOperationArguments bag,
            TurnUiOperationArguments ui,
            TurnGiveItemOperationArguments giveItem,
            TurnQuestOperationArguments quest) {
        this(operation, bag, ui, giveItem, quest, null, null, null);
    }

    public TurnLocalServiceCall(
            TurnLocalOperation operation,
            TurnBagOperationArguments bag,
            TurnUiOperationArguments ui,
            TurnGiveItemOperationArguments giveItem,
            TurnQuestOperationArguments quest,
            TurnWholeTaskRuntimeArguments wholeTaskRuntime) {
        this(operation, bag, ui, giveItem, quest, wholeTaskRuntime, null, null);
    }
}
