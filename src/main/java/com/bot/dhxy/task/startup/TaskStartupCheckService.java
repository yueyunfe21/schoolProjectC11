package com.bot.dhxy.task.startup;

import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.team.TeamRoleDetectionService;
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
        if (!teamRoleDetectionService.shouldRunFiveRing(context)) {
            String prefix = context == null ? "五环" : context.getLogPrefix();
            return TaskStartupCheckResult.skip(prefix + " 当前角色不是五环执行者，跳过五环任务");
        }
        return TaskStartupCheckResult.allow("五环前置判断通过");
    }

    public TaskStartupCheckResult checkAutoBattle(TaskExecutionContext context) {
        if (!teamRoleDetectionService.shouldRunAutoBattle(context)) {
            String prefix = context == null ? "自动战斗" : context.getLogPrefix();
            return TaskStartupCheckResult.skip(prefix + " 当前角色不需要自动战斗，跳过自动战斗任务");
        }
        return TaskStartupCheckResult.allow("自动战斗前置判断通过");
    }
}
