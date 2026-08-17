package com.bot.dhxy.cloud.turn.protocol;

/**
 * Immutable UI-backed settings for one exact remote task start.
 *
 * <p>The Client captures these values when the user starts a task. Cloud binds the snapshot to
 * that exact window/task run; it must not replace a global configuration bean with another
 * window's values.</p>
 */
public record TurnTaskRuntimeSettings(
        boolean summonSkillCleanEnabled,
        long summonSkillCleanIntervalMs,
        long healPetMaintenanceIntervalMs,
        long repairEquipmentMaintenanceIntervalMs,
        boolean maintenanceRunImmediatelyOnStart,
        boolean leaderCommonBoxEnabled,
        boolean memberCommonBoxEnabled,
        boolean taskStartupPreparationEnabled,
        boolean xiuluoSkipBossEnabled,
        boolean doubleExperienceClaimEnabled,
        boolean playerHpSupplyEnabled,
        int playerHpSupplyThreshold,
        boolean playerMpSupplyEnabled,
        int playerMpSupplyThreshold,
        boolean petHpSupplyEnabled,
        int petHpSupplyThreshold,
        boolean petMpSupplyEnabled,
        int petMpSupplyThreshold) {

    /** Baseline used only by compatibility constructors that predate the explicit UI snapshot. */
    public static TurnTaskRuntimeSettings defaults() {
        return new TurnTaskRuntimeSettings(
                true, 20 * 60_000L,
                30 * 60_000L, 55 * 60_000L, false,
                true, false, false, false, true,
                true, 70, true, 70, true, 70, true, 70);
    }
}
