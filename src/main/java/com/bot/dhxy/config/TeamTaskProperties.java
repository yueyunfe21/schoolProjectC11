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

    /**
     * 轻量弹窗/窗口清理是否要求当前角色必须是队员。
     */
    private boolean lightweightCleanupRequiresMember = true;

    /**
     * 队伍身份未知时是否允许轻量弹窗/窗口清理。
     */
    private boolean allowLightweightCleanupWhenRoleUnknown = false;

    /**
     * 是否启用真实队伍身份识别。坐标和模板没填完整时会自动返回 UNKNOWN。
     */
    private boolean roleDetectionEnabled = false;

    private int teamHoverX = 0;
    private int teamHoverY = 0;
    private int teamHoverRandomRadiusX = 20;
    private int teamHoverRandomRadiusY = 12;
    private int teamTooltipRectX = 0;
    private int teamTooltipRectY = 0;
    private int teamTooltipRectW = 0;
    private int teamTooltipRectH = 0;
    private int teamHoverDelayMs = 500;
    /**
     * Maximum hover attempts before declaring no team tooltip.
     *
     * <p>The hover tooltip is timing and pixel-position sensitive during multi-window startup. A
     * single missed tooltip is not reliable enough to decide SOLO, so the detector retries the
     * hover-only probe without opening the team panel.</p>
     */
    private int teamTooltipProbeMaxAttempts = 2;
    private int teamTooltipWhitePixelThreshold = 100;
    private int teamTooltipPurplePixelThreshold = 20;
    private int teamTooltipTextMinRows = 8;
    private int teamTooltipTextMinColumns = 20;
    private int teamTooltipTextMinTransitions = 20;
    private int teamTooltipTextMaxRowCoveragePercent = 80;
    private int teamPanelRoleDetectionMaxAttempts = 2;
    private int teamPanelOpenDelayMs = 500;
    private int teamPanelCloseDelayMs = 150;
    private int teamPanelTransferLeaderRectX = 0;
    private int teamPanelTransferLeaderRectY = 0;
    private int teamPanelTransferLeaderRectW = 0;
    private int teamPanelTransferLeaderRectH = 0;
    private String teamPanelTransferLeaderTemplate = "images/template/team/transfer_leader_button.png";
    private double teamPanelTransferLeaderMatchRate = 0.85;
    private int teamPanelMemberMarkerRectX = 0;
    private int teamPanelMemberMarkerRectY = 0;
    private int teamPanelMemberMarkerRectW = 0;
    private int teamPanelMemberMarkerRectH = 0;
    private String teamPanelMemberMarkerTemplate = "images/template/team/member_marker.png";
    private double teamPanelMemberMarkerMatchRate = 0.85;
    private boolean closeTeamPanelAfterRoleDetection = true;

    public String toLogText() {
        return "fiveRingRequiresLeader=" + fiveRingRequiresLeader
                + " | autoBattleRequiresMember=" + autoBattleRequiresMember
                + " | allowFiveRingWhenRoleUnknown=" + allowFiveRingWhenRoleUnknown
                + " | allowAutoBattleWhenRoleUnknown=" + allowAutoBattleWhenRoleUnknown
                + " | lightweightCleanupRequiresMember=" + lightweightCleanupRequiresMember
                + " | allowLightweightCleanupWhenRoleUnknown=" + allowLightweightCleanupWhenRoleUnknown
                + " | roleDetectionEnabled=" + roleDetectionEnabled;
    }
}
