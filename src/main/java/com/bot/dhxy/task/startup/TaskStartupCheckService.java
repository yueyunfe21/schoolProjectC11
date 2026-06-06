package com.bot.dhxy.task.startup;

import com.bot.dhxy.config.TeamTaskProperties;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.team.TeamRoleDetectionService;
import com.bot.dhxy.team.TeamRoleStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Central task startup gate for in-game role based decisions.
 */
@Slf4j
@Service
public class TaskStartupCheckService {

    private final TeamTaskProperties teamTaskProperties;
    private final TeamRoleDetectionService teamRoleDetectionService;

    public TaskStartupCheckService(TeamTaskProperties teamTaskProperties,
                                   TeamRoleDetectionService teamRoleDetectionService) {
        this.teamTaskProperties = teamTaskProperties;
        this.teamRoleDetectionService = teamRoleDetectionService;
    }

    public TaskStartupCheckResult checkFiveRing(TaskExecutionContext context) {
        if (!teamTaskProperties.isFiveRingRequiresLeader()) {
            return TaskStartupCheckResult.allow(buildReason(context, "five-ring", TeamRoleStatus.UNKNOWN, "role gate disabled"));
        }

        TeamRoleStatus role = teamRoleDetectionService.detectCurrentRole(context);
        if (!teamRoleDetectionService.shouldRunFiveRing(role)) {
            return TaskStartupCheckResult.skip(buildReason(context, "five-ring", role, "current role should skip five-ring"));
        }
        return TaskStartupCheckResult.allow(buildReason(context, "five-ring", role, "allowed"));
    }

    public TaskStartupCheckResult checkAutoBattle(TaskExecutionContext context) {
        if (!teamTaskProperties.isAutoBattleRequiresMember()) {
            return TaskStartupCheckResult.allow(buildReason(context, "auto-battle", TeamRoleStatus.UNKNOWN, "role gate disabled"));
        }

        TeamRoleStatus contextRole = roleFromContext(context);
        if (contextRole.isMember()) {
            return TaskStartupCheckResult.allow(buildReason(context, "auto-battle", contextRole, "allowed by preflight role"));
        }
        if (contextRole.isLeader()) {
            return TaskStartupCheckResult.skip(buildReason(context, "auto-battle", contextRole, "leader should skip auto-battle"));
        }

        /*
         * 自动战斗是手动选择的后台挂机模式，启动时不能为了判定队员身份去 hover 队伍头像或
         * 打开队伍面板。真实队伍检测会抢前台，也会在战斗中启动时误判并直接结束任务。
         * 这里仅使用 window 层已经传下来的 role；没有预判身份时按配置决定是否放行。
         */
        if (teamTaskProperties.isAllowAutoBattleWhenRoleUnknown()) {
            return TaskStartupCheckResult.allow(buildReason(context, "auto-battle", TeamRoleStatus.UNKNOWN,
                    "allowed because live role detection is skipped"));
        }
        return TaskStartupCheckResult.skip(buildReason(context, "auto-battle", TeamRoleStatus.UNKNOWN,
                "role unknown and live role detection is skipped"));
    }

    private TeamRoleStatus roleFromContext(TaskExecutionContext context) {
        if (context == null || context.getWindowRole() == null) {
            return TeamRoleStatus.UNKNOWN;
        }
        if ("MEMBER".equalsIgnoreCase(context.getWindowRole())) {
            return TeamRoleStatus.MEMBER;
        }
        if ("LEADER".equalsIgnoreCase(context.getWindowRole())) {
            return TeamRoleStatus.LEADER;
        }
        return TeamRoleStatus.UNKNOWN;
    }

    private String buildReason(TaskExecutionContext context, String taskName, TeamRoleStatus role, String message) {
        String prefix = context == null ? taskName : context.getLogPrefix();
        TeamRoleStatus safeRole = role == null ? TeamRoleStatus.UNKNOWN : role;
        return prefix + " | role=" + safeRole.name() + " | " + message;
    }
}
