package com.bot.dhxy.task.model;

public enum TaskType {
    WUHuan_V2("wuhuan_v2", "五环"),
    WUBEI("wubei", "五倍"),
    XIULUO("xiuluo", "修罗"),
    XIULUO_V2("xiuluo_v2", "修罗"),
    AUTO_BATTLE("auto_battle", "自动战斗"),
    SLEEP_COMPUTER("sleep_computer", "睡眠计算机"),
    DEBUG_COORDINATE("debug_coordinate", "坐标调试"),
    DEBUG_MAP_CALIBRATOR("debug_map_calibrator", "地图校准"),
    DEBUG_TEAM_ROLE("debug_team_role", "队伍识别测试"),
    DEBUG_XIULUO_STORY_OBJECTIVE("debug_xiuluo_story_objective", "修罗Story目标测试"),
    DEBUG_XIULUO_TASK_PANEL_OBJECTIVE("debug_xiuluo_task_panel_objective", "修罗任务栏目标测试"),
    DEBUG_XIULUO_MOCK_OBJECTIVE("debug_xiuluo_mock_objective", "修罗模拟目标导航测试"),
    DEBUG_NAVIGATION_STRESS("debug_navigation_stress", "导航压力测试"),
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
