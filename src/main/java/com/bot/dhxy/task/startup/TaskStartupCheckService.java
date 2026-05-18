package com.bot.dhxy.task.startup;

import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.team.TeamRoleDetectionService;
import com.bot.dhxy.team.TeamRoleStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 任务启动前置判断统一入口。
 *
 * 以后所有“是不是队长 / 是不是队员 / 当前状态能不能跑”的判断都放任务内部调用这里，
 * 不要放在 window 层。
 */
@Slf4j
@Service
public class TaskStartupCheckService {

    private final TeamRoleDetectionService teamRoleDetectionService;

    public TaskStartupCheckService(TeamRoleDetectionService teamRoleDetectionService) {
        this.teamRoleDetectionService = teamRoleDetectionService;
    }

    public TaskStartupCheckResult checkFiveRing(TaskExecutionContext context) {
        TeamRoleStatus role = teamRoleDetectionService.detectCurrentRole(context);
        if (!teamRoleDetectionService.shouldRunFiveRing(context)) {
            return TaskStartupCheckResult.skip(buildReason(context, "五环", role, "当前角色不是五环执行者，跳过五环任务"));
        }
        return TaskStartupCheckResult.allow(buildReason(context, "五环", role, "允许执行"));
    }

    public TaskStartupCheckResult checkAutoBattle(TaskExecutionContext context) {
        TeamRoleStatus role = teamRoleDetectionService.detectCurrentRole(context);
        if (!teamRoleDetectionService.shouldRunAutoBattle(context)) {
            return TaskStartupCheckResult.skip(buildReason(context, "自动战斗", role, "当前角色不需要自动战斗，跳过自动战斗任务"));
        }
        return TaskStartupCheckResult.allow(buildReason(context, "自动战斗", role, "允许执行"));
    }

    private String buildReason(TaskExecutionContext context, String taskName, TeamRoleStatus role, String message) {
        String prefix = context == null ? taskName : context.getLogPrefix();
        TeamRoleStatus safeRole = role == null ? TeamRoleStatus.UNKNOWN : role;
        return prefix + " | role=" + safeRole.name() + " | " + message;
    }
}
