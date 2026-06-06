package com.bot.dhxy.model.maintenance;

import lombok.Builder;
import lombok.Value;

/**
 * Result for one shared maintenance pass.
 *
 * @param status stable maintenance status for caller-side branching/logging.
 * @param handled true when the pass performed an action that should end the current idle tick.
 * @param broadcastHandled true only when a team-wide maintenance dialog was clicked.
 * @param summonSkillAttempted true when summon-skill cleanup actually opened its focused workflow.
 * @param summonSkillSucceeded true only when summon-skill cleanup completed successfully and its
 *                             cooldown may be refreshed.
 * @param message short diagnostic message.
 */
@Value
@Builder
public class TaskMaintenanceResult {
    TaskMaintenanceStatus status;
    boolean handled;
    boolean broadcastHandled;
    boolean summonSkillAttempted;
    boolean summonSkillSucceeded;
    String message;

    public static TaskMaintenanceResult noAction(String message) {
        return TaskMaintenanceResult.builder()
                .status(TaskMaintenanceStatus.NO_ACTION)
                .message(message)
                .build();
    }

    public static TaskMaintenanceResult broadcastHandled(String message) {
        return TaskMaintenanceResult.builder()
                .status(TaskMaintenanceStatus.BROADCAST_HANDLED)
                .handled(true)
                .broadcastHandled(true)
                .message(message)
                .build();
    }

    public static TaskMaintenanceResult summonSkillCleaned(String message) {
        return TaskMaintenanceResult.builder()
                .status(TaskMaintenanceStatus.SUMMON_SKILL_CLEANED)
                .handled(true)
                .summonSkillAttempted(true)
                .summonSkillSucceeded(true)
                .message(message)
                .build();
    }

    public static TaskMaintenanceResult simple(TaskMaintenanceStatus status, String message) {
        return TaskMaintenanceResult.builder()
                .status(status)
                .message(message)
                .build();
    }
}
