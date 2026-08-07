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
    TIANTING_YINYAO,
    TIANTING_FENGYAO,
    /** All known 天庭 options, armed only after a tracker click starts no movement. */
    TIANTING_RECOVERY_OPTION,
    CLEANUP
}
