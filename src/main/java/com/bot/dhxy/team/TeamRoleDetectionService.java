package com.bot.dhxy.team;

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

    /**
     * 当前默认不拦截任何任务，避免影响现有五环流程。
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
     * 当前默认 true，保证现有五环不被影响。
     * 后续真实识别完成后，可以改成：LEADER / SOLO 允许，MEMBER 跳过。
     */
    public boolean shouldRunFiveRing(TaskExecutionContext context) {
        return true;
    }

    /**
     * 自动战斗是否允许继续执行。
     *
     * 当前默认 true。后续可以按是否队员、是否战斗状态进一步收紧。
     */
    public boolean shouldRunAutoBattle(TaskExecutionContext context) {
        return true;
    }
}
