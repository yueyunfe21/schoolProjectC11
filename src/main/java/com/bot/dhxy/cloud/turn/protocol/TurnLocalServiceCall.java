package com.bot.dhxy.cloud.turn.protocol;

public record TurnLocalServiceCall(
        TurnLocalOperation operation,
        TurnBagOperationArguments bag,
        TurnUiOperationArguments ui,
        TurnGiveItemOperationArguments giveItem,
        TurnQuestOperationArguments quest) {
}
