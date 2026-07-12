package com.bot.dhxy.cloud.task;

import lombok.Builder;
import lombok.Value;

/**
 * CR267 cloud answer to a direct-combat authorization request.
 *
 * <p>{@code ENTER_DIRECT_COMBAT} is an independent scene-transition decision derived from
 * structured task facts only. Local code may press Alt+A exactly once per authorized answer;
 * a refused/disabled/failed answer keeps the existing failure path without any scene switch.</p>
 */
@Value
@Builder
public class NpcClickSmartDirectCombatAuthorization {
    @Builder.Default
    boolean authorized = false;
    String status;
    String reason;
    String decisionId;

    public static NpcClickSmartDirectCombatAuthorization refused(String status, String reason, String decisionId) {
        return NpcClickSmartDirectCombatAuthorization.builder()
                .authorized(false)
                .status(status)
                .reason(reason)
                .decisionId(decisionId)
                .build();
    }
}
