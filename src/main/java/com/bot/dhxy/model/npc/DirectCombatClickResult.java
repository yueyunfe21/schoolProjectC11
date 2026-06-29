package com.bot.dhxy.model.npc;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Result of an Alt+A direct combat click attempt.
 *
 * @param combatEntered true when battle radar or an equivalent verifier confirmed combat entry.
 * @param positionRefreshRequired true when Alt+A was entered and then canceled/failed. Canceling
 *                                direct-combat mode can move the character, so task code must
 *                                rerun its own navigation/current-map approach before another
 *                                combat-target click.
 * @param reason diagnostic reason for logs and task retry decisions.
 */
@Value
@Builder
@Accessors(fluent = true)
public class DirectCombatClickResult {
    boolean combatEntered;
    boolean positionRefreshRequired;
    String reason;

    public static DirectCombatClickResult combatEntered(String reason) {
        return DirectCombatClickResult.builder()
                .combatEntered(true)
                .positionRefreshRequired(false)
                .reason(reason)
                .build();
    }

    public static DirectCombatClickResult skipped(String reason) {
        return DirectCombatClickResult.builder()
                .combatEntered(false)
                .positionRefreshRequired(false)
                .reason(reason)
                .build();
    }

    public static DirectCombatClickResult positionRefreshRequired(String reason) {
        return DirectCombatClickResult.builder()
                .combatEntered(false)
                .positionRefreshRequired(true)
                .reason(reason)
                .build();
    }
}
