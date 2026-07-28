package com.bot.dhxy.cloud.turn.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** Accepted task start plus the Cloud-authoritative effective queue when it is known at acknowledgement time. */
public record TurnTaskStartAck(
        String startRequestId,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<TurnTaskCode> effectiveTaskCodes) {

    public TurnTaskStartAck {
        effectiveTaskCodes = effectiveTaskCodes == null ? null : List.copyOf(effectiveTaskCodes);
    }

    public TurnTaskStartAck(String startRequestId) {
        this(startRequestId, null);
    }
}
