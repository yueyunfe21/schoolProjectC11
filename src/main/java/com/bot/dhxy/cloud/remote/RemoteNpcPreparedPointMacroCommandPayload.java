package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * W-696-NPC-TOOLTIP-PREPAREDPOINT-CONTRACT-COHORT-1: closed DHXY-side wire command for the NPC
 * prepared-point local macro. Mirrors the Cloud {@code NpcPreparedPointMacroCommand} contract exactly:
 * one screen-absolute click point, the first verify wait ({@code >= 0}) and the baseline optional retry
 * count ({@code 0} or {@code 1}); no additional physical input is authorized.
 *
 * <p>Standalone contract type; the sealed transport / kind binding is the deferred shared-integration
 * seam.</p>
 */
@Value
@Jacksonized
public class RemoteNpcPreparedPointMacroCommandPayload {

    int screenX;
    int screenY;
    long firstWaitMs;
    int maxRetries;
    String description;

    @Builder
    public RemoteNpcPreparedPointMacroCommandPayload(int screenX, int screenY, long firstWaitMs, int maxRetries, String description) {
        require(firstWaitMs >= 0L, "firstWaitMs must be non-negative");
        require(maxRetries == 0 || maxRetries == 1, "maxRetries must be 0 or 1");
        this.screenX = screenX;
        this.screenY = screenY;
        this.firstWaitMs = firstWaitMs;
        this.maxRetries = maxRetries;
        this.description = description;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
