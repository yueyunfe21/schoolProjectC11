package com.bot.dhxy.model.npc;

/**
 * CR267 structured route/scenario fact for cloud direct-combat authorization.
 *
 * <p>Cloud allowlists only {@link #WUBEI_PROBE_TARGET_READY} and {@link #LEGACY_COMBAT_TARGET};
 * {@link #TRACKER_SHORTCUT}, a missing value, and any unknown value are always refused. The
 * scenario is a request fact so the contract is enforced by the cloud decision itself, not by
 * which local call sites happen to exist.</p>
 */
public enum NpcDirectCombatScenario {
    /**
     * 五倍白龙马 probe branch whose dedicated {@code wubei.probeTargetReady} result already
     * confirmed the target appeared.
     */
    WUBEI_PROBE_TARGET_READY,

    /**
     * Old-route combat monster (修罗旧路线或其他可战斗怪): canonical map matches and the player is
     * inside the task's existing per-axis coordinate tolerance around the target.
     */
    LEGACY_COMBAT_TARGET,

    /**
     * 新修罗 tracker shortcut route. It only consumes tracker prepared actions and must never
     * enter direct-combat; cloud refuses this value unconditionally. It exists so a future
     * shortcut caller cannot masquerade as an allowlisted scenario.
     */
    TRACKER_SHORTCUT
}
