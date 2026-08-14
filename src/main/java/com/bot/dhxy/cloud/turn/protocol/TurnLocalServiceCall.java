package com.bot.dhxy.cloud.turn.protocol;

public record TurnLocalServiceCall(
        TurnLocalOperation operation,
        TurnBagOperationArguments bag,
        TurnUiOperationArguments ui,
        TurnGiveItemOperationArguments giveItem,
        TurnQuestOperationArguments quest,
        TurnWholeTaskRuntimeArguments wholeTaskRuntime,
        TurnMetricEventPayload metric,
        TurnTaskTrackerOperationArguments taskTracker,
        TurnXinshouDragArguments xinshouDrag,
        TurnXinshouTrackerChainArguments xinshouTrackerChain,
        TurnXinshouMechanicalArguments xinshouMechanical,
        TurnMapLabelTemplateArguments mapLabelTemplate) {

    /** Compatibility constructor for the protocol shape that predates map-label mirroring. */
    public TurnLocalServiceCall(
            TurnLocalOperation operation,
            TurnBagOperationArguments bag,
            TurnUiOperationArguments ui,
            TurnGiveItemOperationArguments giveItem,
            TurnQuestOperationArguments quest,
            TurnWholeTaskRuntimeArguments wholeTaskRuntime,
            TurnMetricEventPayload metric,
            TurnTaskTrackerOperationArguments taskTracker,
            TurnXinshouDragArguments xinshouDrag,
            TurnXinshouTrackerChainArguments xinshouTrackerChain,
            TurnXinshouMechanicalArguments xinshouMechanical) {
        this(operation, bag, ui, giveItem, quest, wholeTaskRuntime, metric, taskTracker,
                xinshouDrag, xinshouTrackerChain, xinshouMechanical, null);
    }

    public TurnLocalServiceCall(
            TurnLocalOperation operation,
            TurnMapLabelTemplateArguments mapLabelTemplate) {
        this(operation, null, null, null, null, null, null, null, null, null, null,
                mapLabelTemplate);
    }

    public TurnLocalServiceCall(
            TurnLocalOperation operation,
            TurnXinshouMechanicalArguments xinshouMechanical) {
        this(operation, null, null, null, null, null, null, null, null, null, xinshouMechanical);
    }

    public TurnLocalServiceCall(
            TurnLocalOperation operation,
            TurnXinshouTrackerChainArguments xinshouTrackerChain) {
        this(operation, null, null, null, null, null, null, null, null, xinshouTrackerChain, null);
    }

    public TurnLocalServiceCall(
            TurnLocalOperation operation,
            TurnBagOperationArguments bag,
            TurnUiOperationArguments ui,
            TurnGiveItemOperationArguments giveItem,
            TurnQuestOperationArguments quest,
            TurnWholeTaskRuntimeArguments wholeTaskRuntime,
            TurnMetricEventPayload metric,
            TurnTaskTrackerOperationArguments taskTracker,
            TurnXinshouDragArguments xinshouDrag,
            TurnXinshouTrackerChainArguments xinshouTrackerChain) {
        this(operation, bag, ui, giveItem, quest, wholeTaskRuntime, metric, taskTracker,
                xinshouDrag, xinshouTrackerChain, null);
    }

    public TurnLocalServiceCall(
            TurnLocalOperation operation,
            TurnBagOperationArguments bag,
            TurnUiOperationArguments ui,
            TurnGiveItemOperationArguments giveItem,
            TurnQuestOperationArguments quest,
            TurnWholeTaskRuntimeArguments wholeTaskRuntime,
            TurnMetricEventPayload metric,
            TurnTaskTrackerOperationArguments taskTracker,
            TurnXinshouDragArguments xinshouDrag) {
        this(operation, bag, ui, giveItem, quest, wholeTaskRuntime, metric, taskTracker,
                xinshouDrag, null, null);
    }

    public TurnLocalServiceCall(
            TurnLocalOperation operation,
            TurnBagOperationArguments bag,
            TurnUiOperationArguments ui,
            TurnGiveItemOperationArguments giveItem,
            TurnQuestOperationArguments quest,
            TurnWholeTaskRuntimeArguments wholeTaskRuntime,
            TurnMetricEventPayload metric,
            TurnTaskTrackerOperationArguments taskTracker) {
        this(operation, bag, ui, giveItem, quest, wholeTaskRuntime, metric, taskTracker,
                null, null, null);
    }

    public TurnLocalServiceCall(
            TurnLocalOperation operation,
            TurnBagOperationArguments bag,
            TurnUiOperationArguments ui,
            TurnGiveItemOperationArguments giveItem,
            TurnQuestOperationArguments quest,
            TurnWholeTaskRuntimeArguments wholeTaskRuntime,
            TurnMetricEventPayload metric) {
        this(operation, bag, ui, giveItem, quest, wholeTaskRuntime, metric, null, null, null, null);
    }

    public TurnLocalServiceCall(
            TurnLocalOperation operation,
            TurnBagOperationArguments bag,
            TurnUiOperationArguments ui,
            TurnGiveItemOperationArguments giveItem,
            TurnQuestOperationArguments quest) {
        this(operation, bag, ui, giveItem, quest, null, null, null, null, null, null);
    }

    public TurnLocalServiceCall(
            TurnLocalOperation operation,
            TurnBagOperationArguments bag,
            TurnUiOperationArguments ui,
            TurnGiveItemOperationArguments giveItem,
            TurnQuestOperationArguments quest,
            TurnWholeTaskRuntimeArguments wholeTaskRuntime) {
        this(operation, bag, ui, giveItem, quest, wholeTaskRuntime, null, null, null, null, null);
    }
}
