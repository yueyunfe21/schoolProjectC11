package com.bot.dhxy.task.model;

public enum TaskType {
    WUHuan_V2("wuhuan_v2", "五环", true),
    WUBEI("wubei", "五倍", false),
    XIULUO("xiuluo", "修罗", false),
    XIULUO_V2("xiuluo_v2", "修罗", false),
    AUTO_BATTLE("auto_battle", "自动战斗", false),
    SLEEP_COMPUTER("sleep_computer", "睡眠计算机", false),
    UNKNOWN("unknown", "未知任务", false);

    private final String code;
    private final String displayName;
    private final boolean singlePlayer;

    TaskType(String code, String displayName, boolean singlePlayer) {
        this.code = code;
        this.displayName = displayName;
        this.singlePlayer = singlePlayer;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSinglePlayer() {
        return singlePlayer;
    }
}
