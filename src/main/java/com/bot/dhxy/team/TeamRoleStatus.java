package com.bot.dhxy.team;

/**
 * 当前游戏角色在队伍里的真实身份。
 *
 * 这个身份应该由游戏画面/后端状态识别得出，不应该由窗口顺序决定。
 */
public enum TeamRoleStatus {
    /** 明确识别为队长。 */
    LEADER,

    /** 明确识别为队员。 */
    MEMBER,

    /** 明确识别为未组队/单人。 */
    SOLO,

    /** 目前还没有识别逻辑或识别失败。 */
    UNKNOWN;

    public boolean isLeader() {
        return this == LEADER;
    }

    public boolean isMember() {
        return this == MEMBER;
    }

    public boolean isSolo() {
        return this == SOLO;
    }

    public boolean isUnknown() {
        return this == UNKNOWN;
    }
}
