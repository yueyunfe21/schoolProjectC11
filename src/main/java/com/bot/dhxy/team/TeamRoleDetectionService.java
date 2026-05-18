package com.bot.dhxy.team;

import com.bot.dhxy.config.TeamTaskProperties;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 游戏内队伍身份识别服务。
 *
 * 注意：window 层不会也不应该判断队长/队员。
 * 后面真正的图像识别/状态识别逻辑应该写在这里，任务自己调用这里决定是否继续执行。
 */
@Slf4j
@Service
public class TeamRoleDetectionService {

    private final TeamTaskProperties teamTaskProperties;

    public TeamRoleDetectionService(TeamTaskProperties teamTaskProperties) {
        this.teamTaskProperties = teamTaskProperties;
    }

    /**
     * 当前默认不做真实识别，返回 UNKNOWN。
     *
     * 后续补上真实识别后，可以返回 LEADER / MEMBER / SOLO / UNKNOWN。
     */
    public TeamRoleStatus detectCurrentRole(TaskExecutionContext context) {
        if (context != null && context.hasWindow()) {
            log.debug("队伍身份识别暂未启用：{}", context.getLogPrefix());
        }
        return TeamRoleStatus.UNKNOWN;
    }

    /**
     * 五环是否允许继续执行。
     *
     * 规则只在任务内部生效，window 层不参与队长/队员判断。
     */
    public boolean shouldRunFiveRing(TaskExecutionContext context) {
        TeamRoleStatus role = detectCurrentRole(context);
        if (!teamTaskProperties.isFiveRingRequiresLeader()) {
            return true;
        }
        if (role.isLeader()) {
            return true;
        }
        return role.isUnknown() && teamTaskProperties.isAllowFiveRingWhenRoleUnknown();
    }

    /**
     * 自动战斗是否允许继续执行。
     */
    public boolean shouldRunAutoBattle(TaskExecutionContext context) {
        TeamRoleStatus role = detectCurrentRole(context);
        if (!teamTaskProperties.isAutoBattleRequiresMember()) {
            return true;
        }
        if (role.isMember()) {
            return true;
        }
        return role.isUnknown() && teamTaskProperties.isAllowAutoBattleWhenRoleUnknown();
    }

    public String describeCurrentRole(TaskExecutionContext context) {
        return detectCurrentRole(context).name();
    }
}
