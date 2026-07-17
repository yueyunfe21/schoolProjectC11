package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnResponse;

import java.util.Objects;

/**
 * Successful HTTP turn exchange. Construction is restricted to the one protocol acknowledgement state.
 *
 * @param response validated Cloud response, including either ACTION or IDLE
 * @param previousOutcomeStatus acknowledgement for the previous outcome carried by the request
 */
public record TurnExchangeResult(
        TurnResponse response,
        PreviousOutcomeStatus previousOutcomeStatus) {

    public TurnExchangeResult {
        Objects.requireNonNull(response, "response");
        if (previousOutcomeStatus != PreviousOutcomeStatus.ACCEPTED) {
            throw new IllegalArgumentException("a successful turn exchange must accept the previous outcome");
        }
    }

    public static TurnExchangeResult accepted(TurnResponse response) {
        return new TurnExchangeResult(response, PreviousOutcomeStatus.ACCEPTED);
    }

    public enum PreviousOutcomeStatus {
        ACCEPTED
    }
}
