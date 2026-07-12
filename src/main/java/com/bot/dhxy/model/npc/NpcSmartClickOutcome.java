package com.bot.dhxy.model.npc;

import lombok.Builder;
import lombok.Value;

/**
 * CR267 structured terminal of one ordinary {@code NPC_CLICK_SMART} attempt.
 *
 * <p>{@code normalFifoConsumedUnverified} is the auditable direct-combat gate fact: it is true
 * ONLY when the cloud FIFO queue reached its genuine {@code END} message with every candidate
 * consumed and none verified. Cloud-inactive, session-start failure, invalid/protocol messages,
 * stop/cancel, WAIT timeout, and candidate-budget exhaustion all keep it false — those terminals
 * do not prove the ordinary FIFO was fully consumed, so they must never authorize
 * {@code ENTER_DIRECT_COMBAT}.</p>
 */
@Value
@Builder
public class NpcSmartClickOutcome {
    @Builder.Default
    boolean verified = false;
    @Builder.Default
    boolean normalFifoConsumedUnverified = false;
    /**
     * Terminal status name for logs/audit, e.g. {@code CLOUD_EXECUTED}, {@code CLOUD_NO_ACTION},
     * {@code DISABLED}, {@code REQUIRED_FAILURE}.
     */
    String terminalStatus;
}
