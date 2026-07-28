package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.TurnFrame;
import com.bot.dhxy.cloud.turn.protocol.TurnContinuationDecision;
import com.bot.dhxy.cloud.turn.protocol.TurnContinuationRequest;

/** Same-endpoint callback bound to the currently executing unresolved action. */
@FunctionalInterface
public interface TurnContinuationGateway {
    TurnContinuationDecision exchange(TurnContinuationRequest request, TurnFrame frame);
}
