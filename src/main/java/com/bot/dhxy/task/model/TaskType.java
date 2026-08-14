package com.bot.dhxy.task.model;

public enum TaskType {
    WUHuan_V2("wuhuan_v2", "五环", true),
    WUHUAN_V3("wuhuan_v3", "wuhuanV3", true),
    WUBEI("wubei", "五倍", false),
    XIULUO("xiuluo", "修罗", false),
    XIULUO_V2("xiuluo_v2", "修罗", false),
    XINSHOU("xinshou", "新手", true),
    XINSHOU_TRAINING("xinshou_training", "江湖历练", false),
    CATCH_GHOST("catch_ghost", "抓鬼", false),
    GHOST_KING("ghost_king", "鬼王", false),
    YIPIN_GUARD_TEST("yipin_guard_test", "测试一品侍卫接任务", false),
    G056_DOUBLE_EXPERIENCE_ACCEPTANCE("g056_double_experience_acceptance", "G056领双验收", false),
    WILD_BATTLE("wild_battle", "野外战斗", false),
    TIANTING("tianting", "天庭", false),
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
