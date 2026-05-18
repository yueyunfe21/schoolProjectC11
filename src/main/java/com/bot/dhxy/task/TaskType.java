package com.bot.dhxy.task;

/**
 * 用户可选择的任务类型。
 */
public enum TaskType {
    WUHuan("wuhuan", "五环"),
    AUTO_BATTLE("auto_battle", "自动战斗"),
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
