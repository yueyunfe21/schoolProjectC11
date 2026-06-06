package com.bot.dhxy.model.maintenance;

import lombok.Builder;
import lombok.Value;

/**
 * Request for one opportunistic maintenance pass.
 *
 * @param sourceTask diagnostic task/source name written to logs.
 * @param handleMaintenanceBroadcast whether to handle team-wide maintenance option dialogs such as
 *                                   heal-pet or repair-equipment prompts.
 * @param cleanSummonSkill whether this pass may run the focused summon-skill cleanup.
 * @param requireFreeStateForSummonSkill when true, summon cleanup is skipped unless the current
 *                                       {@code GameContext.ActionState} is FREE.
 * @param oneSummonSkillPerTeamRound whether summon cleanup must claim a shared team-round slot.
 * @param maxSummonSkillCleanersPerTeamRound maximum different windows allowed to run summon cleanup
 *                                           in the same team round when the round gate is enabled.
 * @param teamMaintenanceKey stable team task key, for example {@code xiuluo_v2}; null falls back to
 *                           the current task/requested-task code.
 * @param teamRound one-based team round number. Null uses the latest round registered for the
 *                  teamMaintenanceKey.
 */
@Value
@Builder(toBuilder = true)
public class TaskMaintenanceRequest {
    String sourceTask;

    @Builder.Default
    boolean handleMaintenanceBroadcast = true;

    @Builder.Default
    boolean cleanSummonSkill = false;

    @Builder.Default
    boolean requireFreeStateForSummonSkill = true;

    @Builder.Default
    boolean oneSummonSkillPerTeamRound = false;

    @Builder.Default
    int maxSummonSkillCleanersPerTeamRound = 1;

    String teamMaintenanceKey;
    Integer teamRound;
}
