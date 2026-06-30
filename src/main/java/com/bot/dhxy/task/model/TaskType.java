package com.bot.dhxy.task.model;

public enum TaskType {
    WUHuan_V2("wuhuan_v2", "五环"),
    WUBEI("wubei", "五倍"),
    XIULUO("xiuluo", "修罗"),
    XIULUO_V2("xiuluo_v2", "修罗"),
    AUTO_BATTLE("auto_battle", "自动战斗"),
    SLEEP_COMPUTER("sleep_computer", "睡眠计算机"),
    UNKNOWN("unknown", "未知任务");

    private final String code;
    private final String displayName;

    TaskType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }
}
