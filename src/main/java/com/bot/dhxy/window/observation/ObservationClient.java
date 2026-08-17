package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.protocol.observation.ObservationRequest;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationResponse;

/**
 * TURN-40G: transport boundary for the independent observation plane. Implementations never touch the command
 * plane's turn endpoint or its per-window unresolved action slot; they carry observation samples up and interest
 * revisions, event acknowledgements and analysis results back.
 */
public interface ObservationClient {

    /**
     * Sends one observation request exactly once.
     *
     * @param request validated per-window observation request; non-null
     * @return a validated observation response cross-checked against the request
     * @throws ObservationTransportException on configuration, serialization, network, HTTP, bound, parse, or
     *         contract failure; a transport failure is never interpreted as a business fact
     */
    ObservationResponse send(ObservationRequest request) throws ObservationTransportException;

    /**
     * Sends with a dedicated cancellation token. Production HTTPS overrides this boundary; the
     * default preserves the functional interface used by bounded in-process transports.
     */
    default ObservationResponse send(
            ObservationRequest request,
            ObservationSendCancellation cancellation) throws ObservationTransportException {
        if (cancellation != null && cancellation.isCancelled()) {
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.INTERRUPTED,
                    "observation HTTP request cancelled before send");
        }
        return send(request);
    }
}
