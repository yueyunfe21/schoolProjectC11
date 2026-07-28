package com.bot.dhxy.window.observation;

/**
 * Handles one passive maintenance-broadcast prompt entirely on the bound client window.
 *
 * <p>This boundary deliberately returns only whether local input completed. A passive member
 * response is not a Cloud business event and must never refresh the leader's broadcast cooldown.</p>
 */
public interface LocalMaintenanceBroadcastHandler {

    /**
     * Capture, match and click at most one visible maintenance-broadcast option.
     *
     * @return {@code true} only when the local input queue completed the matching click.
     */
    boolean handleIfPresent();
}
