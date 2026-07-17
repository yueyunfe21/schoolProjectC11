package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Closed wire command for the {@code LOCAL_MACRO / PLAYER_STATE_FIRST_AID} macro. Mirrors the Cloud
 * closed contract: exactly one of three operations, each carrying only its own variant fields.
 *
 * <ul>
 *   <li>{@code PROBE_SUPPLY_NO_FOCUS} and {@code HEAL_ALL} carry exactly the four
 *       {@code enabled + raw threshold} bar toggles (playerHp/playerMp/petHp/petMp) and no cached-plan
 *       fields.</li>
 *   <li>{@code EXECUTE_CACHED_PLAN} carries exactly {@code planBaseX/planBaseY} plus a non-empty ordered
 *       target list, and no bar toggles.</li>
 * </ul>
 *
 * <p>Variant fields not used by the chosen operation must be explicitly {@code null}/empty; the
 * constructor rejects any mixed shape. Carries no owner/session/queue/retry.</p>
 */
@Value
@Jacksonized
public class RemotePlayerStateFirstAidMacroCommandPayload implements RemoteLocalMacroCommandPayload {
    RemoteLocalMacroKind macroKind;
    Operation operation;
    RemoteFirstAidToggle playerHp;
    RemoteFirstAidToggle playerMp;
    RemoteFirstAidToggle petHp;
    RemoteFirstAidToggle petMp;
    Integer planBaseX;
    Integer planBaseY;
    List<RemoteCachedFirstAidTarget> targets;

    @Builder
    public RemotePlayerStateFirstAidMacroCommandPayload(
            RemoteLocalMacroKind macroKind,
            Operation operation,
            RemoteFirstAidToggle playerHp,
            RemoteFirstAidToggle playerMp,
            RemoteFirstAidToggle petHp,
            RemoteFirstAidToggle petMp,
            Integer planBaseX,
            Integer planBaseY,
            List<RemoteCachedFirstAidTarget> targets) {
        if (macroKind != RemoteLocalMacroKind.PLAYER_STATE_FIRST_AID) {
            throw new IllegalArgumentException("macroKind must be PLAYER_STATE_FIRST_AID");
        }
        if (operation == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        boolean hasAllToggles = playerHp != null && playerMp != null && petHp != null && petMp != null;
        boolean hasAnyToggle = playerHp != null || playerMp != null || petHp != null || petMp != null;
        boolean hasCachedBase = planBaseX != null || planBaseY != null;
        boolean hasTargets = targets != null && !targets.isEmpty();
        switch (operation) {
            case PROBE_SUPPLY_NO_FOCUS, HEAL_ALL -> {
                if (!hasAllToggles) {
                    throw new IllegalArgumentException(operation + " requires all four bar toggles");
                }
                // Match the Cloud command exactly: the toggle operations require targets == null (an
                // explicitly empty list is also rejected), not merely a non-empty list absent.
                if (hasCachedBase || targets != null) {
                    throw new IllegalArgumentException(operation + " must not carry cached-plan fields");
                }
            }
            case EXECUTE_CACHED_PLAN -> {
                if (planBaseX == null || planBaseY == null || !hasTargets) {
                    throw new IllegalArgumentException(
                            "EXECUTE_CACHED_PLAN requires planBaseX/planBaseY and a non-empty target list");
                }
                if (hasAnyToggle) {
                    throw new IllegalArgumentException("EXECUTE_CACHED_PLAN must not carry bar toggles");
                }
            }
        }
        this.macroKind = macroKind;
        this.operation = operation;
        this.playerHp = playerHp;
        this.playerMp = playerMp;
        this.petHp = petHp;
        this.petMp = petMp;
        this.planBaseX = planBaseX;
        this.planBaseY = planBaseY;
        this.targets = targets == null ? null : List.copyOf(targets);
    }

    public enum Operation {
        PROBE_SUPPLY_NO_FOCUS,
        HEAL_ALL,
        EXECUTE_CACHED_PLAN
    }

    /** One bar toggle: whether the bar is enabled and its raw (pre-normalization) threshold percent. */
    @Value
    @Jacksonized
    @Builder
    public static class RemoteFirstAidToggle {
        boolean enabled;
        int threshold;
    }

    /** One ordered cached target: window-relative right-click point and its raw threshold percent. */
    @Value
    @Jacksonized
    public static class RemoteCachedFirstAidTarget {
        String name;
        int relX;
        int relY;
        int threshold;

        @Builder
        public RemoteCachedFirstAidTarget(String name, int relX, int relY, int threshold) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("cached target name must not be blank");
            }
            this.name = name;
            this.relX = relX;
            this.relY = relY;
            this.threshold = threshold;
        }
    }
}
