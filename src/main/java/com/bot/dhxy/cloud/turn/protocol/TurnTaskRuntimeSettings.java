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
        int petMpSupplyThreshold,
        /*
         * 队长死亡恢复出口（用户 2026-08-17 定的两条线路，2026-08-20 接进本快照，按窗口生效）：
         * CONTINUE_TASK = 归队后继续做这个任务（先验任务 title 还在，不在则自动降级为重接）；
         * REACCEPT_TASK = 放弃本次任务，走回起点重新接。
         */
        String leaderDeathRecoveryMode) {

    /** 队长死亡恢复出口：归队后继续做当前任务。 */
    public static final String LEADER_DEATH_RECOVERY_CONTINUE_TASK = "CONTINUE_TASK";
    /** 队长死亡恢复出口：放弃当前任务，走回起点重新接。 */
    public static final String LEADER_DEATH_RECOVERY_REACCEPT_TASK = "REACCEPT_TASK";

    /** Baseline used only by compatibility constructors that predate the explicit UI snapshot. */
    public static TurnTaskRuntimeSettings defaults() {
        return new TurnTaskRuntimeSettings(
                true, 20 * 60_000L,
                30 * 60_000L, 55 * 60_000L, false,
                true, false, false, false, true,
                true, 70, true, 70, true, 70, true, 70,
                LEADER_DEATH_RECOVERY_CONTINUE_TASK);
    }
}
