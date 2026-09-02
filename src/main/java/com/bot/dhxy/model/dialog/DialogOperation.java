package com.bot.dhxy.model.dialog;

public enum DialogOperation {
    INSPECT,
    GIVE_ITEM_IF_AVAILABLE,
    CLICK_KEYWORD,
    CLICK_REMEMBERED_OPTION,
    VERIFY_EXPECTED_DIALOG,
    CLICK_BUSINESS_OPTION,
    CLICK_GREEN_TEMPLATE,
    WUHUAN_SHOE_SHOP_BUY_OPTION,
    VERIFY_WHITE_TEMPLATE,
    ROUTE_TRANSFER,
    TASK_TRACKER_PATHING,
    WUBEI_ACCEPT_TASK,
    WUBEI_ENTER_BATTLE,
    WUBEI_PROBE_STORY,
    XIULUO_ENTER_BATTLE,
    READ_STORY_OBJECTIVE,
    ACCEPT_TASK,
    /** 天庭 combat-entry options matched and clicked entirely on the client. */
    TIANTING_COMBAT_OPTION,
    TIANTING_ACCEPT_TASK,
    /** 取消任务 on 李靖's dialog, armed only when Cloud explicitly requests cancellation. */
    TIANTING_CANCEL_TASK,
    TIANTING_YINYAO,
    DALISI_QUIZ_ACCEPT,
    TIANTING_FENGYAO,
    /** All known 天庭 options, armed only after a tracker click starts no movement. */
    TIANTING_RECOVERY_OPTION,
    /** 天庭 recovery after 引妖香 was already consumed in this accepted quest cycle. */
    TIANTING_RECOVERY_OPTION_NO_YINYAO,
    /** 鬼王接任务选项；地藏王输入前由 Cloud 显式武装。 */
    GHOST_KING_ACCEPT_TASK,
    CLEANUP
}
