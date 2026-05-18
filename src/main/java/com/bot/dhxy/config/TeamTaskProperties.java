package com.bot.dhxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 任务内部的队伍身份前置判断配置。
 *
 * 注意：window 层不使用这些配置。window 层只管窗口。
 * 这些配置只给具体任务启动前判断使用。
 */
@Data
@Component
@ConfigurationProperties(prefix = "bot.team")
public class TeamTaskProperties {

    /**
     * 五环是否要求当前角色必须是队长。
     *
     * 当前默认 false，避免影响现有五环流程。
     * 等真实队长识别稳定后，可以改成 true。
     */
    private boolean fiveRingRequiresLeader = false;

    /**
     * 自动战斗是否要求当前角色必须是队员。
     *
     * 当前默认 false，避免影响现有自动战斗流程。
     */
    private boolean autoBattleRequiresMember = false;

    /**
     * 身份未知时是否允许五环继续执行。
     *
     * 当前默认 true，因为队伍身份识别还没实现。
     */
    private boolean allowFiveRingWhenRoleUnknown = true;

    /**
     * 身份未知时是否允许自动战斗继续执行。
     */
    private boolean allowAutoBattleWhenRoleUnknown = true;

    public String toLogText() {
        return "fiveRingRequiresLeader=" + fiveRingRequiresLeader
                + " | autoBattleRequiresMember=" + autoBattleRequiresMember
                + " | allowFiveRingWhenRoleUnknown=" + allowFiveRingWhenRoleUnknown
                + " | allowAutoBattleWhenRoleUnknown=" + allowAutoBattleWhenRoleUnknown;
    }
}
