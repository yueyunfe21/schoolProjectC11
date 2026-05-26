package com.bot.dhxy.task;

import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.team.TeamRoleDetectionService;
import com.bot.dhxy.team.TeamRoleStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DebugTeamRoleTask implements GameTask {

    private final TeamRoleDetectionService teamRoleDetectionService;

    public DebugTeamRoleTask(TeamRoleDetectionService teamRoleDetectionService) {
        this.teamRoleDetectionService = teamRoleDetectionService;
    }

    @Override
    public String getTaskCode() {
        return "debug_team_role";
    }

    @Override
    public String getTaskName() {
        return "队伍识别测试";
    }

    @Override
    public TaskRunResult execute() {
        return execute(null);
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext context) {
        String prefix = context == null ? "[window=unknown]" : context.getLogPrefix();
        log.info("{} [队伍识别测试] start", prefix);
        TeamRoleStatus role = teamRoleDetectionService.detectCurrentRoleForDebug(context);
        log.info("{} [队伍识别测试] result={}", prefix, role);
        return role == TeamRoleStatus.UNKNOWN ? TaskRunResult.SKIPPED : TaskRunResult.SUCCESS;
    }

    @Override
    public void stop() {
        // One-shot debug task; nothing to stop.
    }
}
