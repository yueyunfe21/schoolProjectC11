package com.bot.dhxy.cloud.turn.protocol;

import java.util.List;

/** Closed local candidate chain for one Cloud-parsed newcomer tracker panel. */
public record TurnXinshouTrackerChainArguments(
        String source,
        List<TurnXinshouTrackerLink> links,
        int sourceWindowLeft,
        int sourceWindowTop,
        int sourceWindowWidth,
        int sourceWindowHeight) {

    public TurnXinshouTrackerChainArguments {
        links = List.copyOf(links == null ? List.of() : links);
    }

    /**
     * Source-compatible constructor for callers that have not yet been taught the capture geometry.
     *
     * <p>The zero-size rectangle is intentionally invalid under {@link TurnProtocolValidator};
     * compatibility must never silently restore the unsafe absolute-click behavior.</p>
     */
    public TurnXinshouTrackerChainArguments(
            String source,
            List<TurnXinshouTrackerLink> links) {
        this(source, links, 0, 0, 0, 0);
    }
}
