package com.bot.dhxy.task.startup;

import com.bot.dhxy.config.TeamTaskProperties;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.team.TeamRoleStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Maps a requested task to the task a window should actually run after role detection.
 *
 * <p>The policy keeps member windows out of leader-only or leader/solo main-task flows by assigning
 * them to the normal auto-battle task. Solo or unknown windows are allowed to run tasks that support solo
 * mode, but are blocked from strict leader-only tasks.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskTeamAssignmentPolicy {

    private final TeamTaskProperties teamTaskProperties;

    /**
     * Resolve the effective task for one detected role.
     *
     * @param requestedTaskType task selected by the UI/user; null is treated as UNKNOWN.
     * @param role live role detected from the game client; null is treated as UNKNOWN.
     * @return effective task type. UNKNOWN means startup should not dispatch the requested task for
     * this window.
     */
    public TaskType resolveTaskForRole(TaskType requestedTaskType, TeamRoleStatus role) {
        TaskType safeTaskType = requestedTaskType == null ? TaskType.UNKNOWN : requestedTaskType;
        TeamRoleStatus safeRole = role == null ? TeamRoleStatus.UNKNOWN : role;

        if (safeTaskType == TaskType.UNKNOWN || safeTaskType == TaskType.AUTO_BATTLE) {
            return safeTaskType;
        }

        if (safeRole.isMember()
                && (isFiveRingTask(safeTaskType) || isLeaderOnlyTask(safeTaskType))) {
            log.info("task team assignment: member window receives normal auto-battle instead of {}", safeTaskType);
            return TaskType.AUTO_BATTLE;
        }

        if ((safeRole.isSolo() || safeRole.isUnknown()) && isLeaderOnlyTask(safeTaskType)) {
            log.info("task team assignment: {} window cannot run leader-only task {}", safeRole, safeTaskType);
            return TaskType.UNKNOWN;
        }

        return safeTaskType;
    }

    /**
     * Decide whether startup must perform live team-role detection for a requested task.
     *
     * @param requestedTaskType task selected by the UI/user; null is treated as UNKNOWN.
     * @return true for tasks whose leader/member/solo semantics affect dispatch.
     */
    public boolean shouldDetectRoleBeforeStart(TaskType requestedTaskType) {
        TaskType safeTaskType = requestedTaskType == null ? TaskType.UNKNOWN : requestedTaskType;
        /*
         * Five Ring may still run for leader/solo/unknown windows, but member windows must be
         * reassigned to auto-battle before the task executes. Therefore Five Ring always needs the
         * role preflight when possible; UNKNOWN is still allowed by resolveTaskForRole.
         */
        return isFiveRingTask(safeTaskType) || isLeaderOnlyTask(safeTaskType);
    }

    private boolean isFiveRingTask(TaskType taskType) {
        return taskType == TaskType.WUHuan_V2;
    }

    private boolean isLeaderOnlyTask(TaskType taskType) {
        return taskType == TaskType.XIULUO
                || taskType == TaskType.XIULUO_V2
                || taskType == TaskType.WUBEI
                || taskType == TaskType.DEBUG_XIULUO_MOCK_OBJECTIVE;
    }
}
