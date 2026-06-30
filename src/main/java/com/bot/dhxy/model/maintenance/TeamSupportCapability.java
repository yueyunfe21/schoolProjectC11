package com.bot.dhxy.model.maintenance;

/**
 * Capability opened by a local leader for member support windows in the same UI-started session.
 */
public enum TeamSupportCapability {
    /** HP/MP first-aid only; must not imply summon skill, repair, common box, or return-team click. */
    FIRST_AID,

    /** Leader has submitted real pathing and opened the wider pathing maintenance opportunity. */
    PATHING_WINDOW,

    /** Permission for member summon-skill cleanup during a wider leader pathing release. */
    SUMMON_SKILL,

    /** Permission for member left-top status cleanup during a wider leader pathing release. */
    LEFT_TOP_STATUS,

    /** Explicit permission for task-agnostic common-box consumption during a released window. */
    COMMON_BOX,

    /** Leader-release capability for actual return-team clicks. */
    TEAM_RETURN
}
