package com.bot.dhxy.task.model;

public enum TaskType {
    WUHuan("wuhuan", "五环"),
    XIULUO("xiuluo", "修罗"),
    XIULUO_V2("xiuluo_v2", "修罗V2"),
    AUTO_BATTLE("auto_battle", "自动战斗"),
    DEBUG_COORDINATE("debug_coordinate", "坐标调试"),
    DEBUG_MAP_CALIBRATOR("debug_map_calibrator", "地图校准"),
    DEBUG_TEAM_ROLE("debug_team_role", "队伍识别测试"),
    DEBUG_XIULUO_STORY_OBJECTIVE("debug_xiuluo_story_objective", "\u4fee\u7f57Story\u76ee\u6807\u6d4b\u8bd5"),
    DEBUG_XIULUO_TASK_PANEL_OBJECTIVE("debug_xiuluo_task_panel_objective", "\u4fee\u7f57\u4efb\u52a1\u680f\u76ee\u6807\u6d4b\u8bd5"),
    DEBUG_XIULUO_MOCK_OBJECTIVE("debug_xiuluo_mock_objective", "\u4fee\u7f57\u6a21\u62df\u76ee\u6807\u5bfc\u822a\u6d4b\u8bd5"),
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
