package com.bot.dhxy.cloud.turn.protocol;

import java.util.List;

public record TurnAction(
        int contractVersion,
        String actionId,
        String deviceId,
        String windowId,
        List<TurnStep> steps,
        boolean fullWindowFailureEvidence,
        TurnPathingIntent pathingIntent) {

    /**
     * Backward-compatible constructor for actions that carry no start-action pathing intent. On the
     * Local Pathing Fact Bridge only a Cloud start action attaches a {@link TurnPathingIntent}; every
     * other action leaves it null.
     */
    public TurnAction(
            int contractVersion,
            String actionId,
            String deviceId,
            String windowId,
            List<TurnStep> steps,
            boolean fullWindowFailureEvidence) {
        this(contractVersion, actionId, deviceId, windowId, steps, fullWindowFailureEvidence, null);
    }
}
