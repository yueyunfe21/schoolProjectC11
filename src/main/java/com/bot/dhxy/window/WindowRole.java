package com.bot.dhxy.window;

/**
 * 单个游戏窗口当前识别到的角色身份。
 *
 * 注意：这里不负责修改游戏里的队长，只描述程序识别到的状态。
 */
public enum WindowRole {
    /** 队长窗口，负责跑主任务、寻路、点 NPC。 */
    LEADER,

    /** 队员窗口，只负责自动战斗、补状态、处理简单弹窗。 */
    MEMBER,

    /** 暂时无法判断身份。 */
    UNKNOWN
}
