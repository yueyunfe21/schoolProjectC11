package com.bot.dhxy.window.model;

/**
 * Window-level observation state for a navigation/pathing handoff.
 */
public enum WindowPathingState {
    NONE,
    ACTIVE,
    ARRIVED,
    STOPPED_AWAY,
    UNKNOWN,
    /** 2026-08-23 停稳事实重设计：本地字模读值判定坐标已停稳（业务判定在云端）。 */
    STABLE,
    /** 数字框不可读第三态（遮挡/黑帧/定位失败），不算动也不算停。 */
    STRIP_UNAVAILABLE
}
