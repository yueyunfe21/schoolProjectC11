package com.bot.dhxy.model.job;

/**
 * CR253 typed prepared work kinds consumed by the 修罗 green-chain foreground.
 *
 * <p>Each type has exactly one producer and one consumer. The foreground only executes physical
 * input for a job that still matches the current window/taskRun/round/attempt identity; everything
 * else is discarded without input.</p>
 */
public enum PreparedActionJobType {
    /**
     * Cloud stop-static 看打 hit: background arbitration recognized the enter-battle option on the
     * stopped static image. The foreground clicks the cloud coordinate once and immediately hands
     * over to WAIT_COMBAT.
     */
    XIULUO_ENTER_BATTLE,
    /**
     * The cloud explicitly returned fallback (CLOUD_NO_ACTION) for the current attempt's stop
     * static. The foreground re-presses the saved green link, creating the next attemptId; only
     * executed re-presses consume the CR232 fallback budget.
     */
    TRACKER_GREEN_RETRY,
    /**
     * Leader summon-skill maintenance became due while the green-chain foreground was parked. The
     * consumer runs the complete three-skill flow, then parks again.
     */
    SUMMON_SKILL_CLEANUP
}
