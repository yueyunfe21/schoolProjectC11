package com.bot.dhxy.cloud.turn.protocol;

public record TurnUiOperationArguments(String source, boolean returnImmediatelyAfterClick) {

    public TurnUiOperationArguments(String source) {
        this(source, false);
    }
}
