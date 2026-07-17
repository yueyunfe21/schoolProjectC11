package com.bot.dhxy.cloud.remote;

public interface RemoteCommandTransport {

    /**
     * Long-polls for at most one command assigned to the request's tenant, device, and client session.
     *
     * @param request immutable poll identity and server wait timeout in milliseconds
     * @return an IDLE or COMMAND response
     */
    RemoteCommandPollResponse poll(RemoteCommandPollRequest request);

    /**
     * Reports one terminal handler outcome without retrying or changing its execution semantics.
     *
     * @param outcome terminal outcome correlated to the command previously returned by poll
     * @return ACCEPTED or DUPLICATE acknowledgement; REJECTED is raised as a transport exception
     */
    RemoteCommandOutcomeAck submitOutcome(RemoteGameOutcomeEnvelope outcome);

    /**
     * Submits one exact locally applied final-consumed receipt without internal retry.
     *
     * @param receipt retained receipt bytes from the local operation ledger
     * @return exact cloud compaction acknowledgement
     */
    RemoteFinalConsumedReceiptAck submitFinalConsumedReceipt(
            RemoteFinalConsumedReceipt receipt);
}
