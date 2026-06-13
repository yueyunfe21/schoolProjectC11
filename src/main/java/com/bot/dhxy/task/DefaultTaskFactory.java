package com.bot.dhxy.task;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.task.wubei.WubeiTask;
import com.bot.dhxy.task.wuhuan.FiveRingTaskV2;
import com.bot.dhxy.task.xiuluo.XiuluoTaskV2;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultTaskFactory implements TaskFactory {

    private final ObjectProvider<FiveRingTaskV2> fiveRingTaskV2Provider;
    private final ObjectProvider<WubeiTask> wubeiTaskProvider;
    private final ObjectProvider<XiuluoTaskV2> xiuluoTaskV2Provider;
    private final ObjectProvider<AutoBattleTask> autoBattleTaskProvider;
    private final ObjectProvider<DebugCoordinateTask> debugCoordinateTaskProvider;
    private final ObjectProvider<DebugMapCalibratorTask> debugMapCalibratorTaskProvider;
    private final ObjectProvider<DebugTeamRoleTask> debugTeamRoleTaskProvider;
    private final ObjectProvider<DebugXiuluoStoryObjectiveTask> debugXiuluoStoryObjectiveTaskProvider;
    private final ObjectProvider<DebugXiuluoTaskPanelObjectiveTask> debugXiuluoTaskPanelObjectiveTaskProvider;
    private final ObjectProvider<DebugXiuluoMockObjectiveTask> debugXiuluoMockObjectiveTaskProvider;
    private final ObjectProvider<DebugNavigationStressTask> debugNavigationStressTaskProvider;

    public DefaultTaskFactory(ObjectProvider<FiveRingTaskV2> fiveRingTaskV2Provider,
                              ObjectProvider<WubeiTask> wubeiTaskProvider,
                              ObjectProvider<XiuluoTaskV2> xiuluoTaskV2Provider,
                              ObjectProvider<AutoBattleTask> autoBattleTaskProvider,
                              ObjectProvider<DebugCoordinateTask> debugCoordinateTaskProvider,
                              ObjectProvider<DebugMapCalibratorTask> debugMapCalibratorTaskProvider,
                              ObjectProvider<DebugTeamRoleTask> debugTeamRoleTaskProvider,
                              ObjectProvider<DebugXiuluoStoryObjectiveTask> debugXiuluoStoryObjectiveTaskProvider,
                              ObjectProvider<DebugXiuluoTaskPanelObjectiveTask> debugXiuluoTaskPanelObjectiveTaskProvider,
                              ObjectProvider<DebugXiuluoMockObjectiveTask> debugXiuluoMockObjectiveTaskProvider,
                              ObjectProvider<DebugNavigationStressTask> debugNavigationStressTaskProvider) {
        this.fiveRingTaskV2Provider = fiveRingTaskV2Provider;
        this.wubeiTaskProvider = wubeiTaskProvider;
        this.xiuluoTaskV2Provider = xiuluoTaskV2Provider;
        this.autoBattleTaskProvider = autoBattleTaskProvider;
        this.debugCoordinateTaskProvider = debugCoordinateTaskProvider;
        this.debugMapCalibratorTaskProvider = debugMapCalibratorTaskProvider;
        this.debugTeamRoleTaskProvider = debugTeamRoleTaskProvider;
        this.debugXiuluoStoryObjectiveTaskProvider = debugXiuluoStoryObjectiveTaskProvider;
        this.debugXiuluoTaskPanelObjectiveTaskProvider = debugXiuluoTaskPanelObjectiveTaskProvider;
        this.debugXiuluoMockObjectiveTaskProvider = debugXiuluoMockObjectiveTaskProvider;
        this.debugNavigationStressTaskProvider = debugNavigationStressTaskProvider;
    }

    @Override
    public GameTask createTask(WindowRuntimeContext windowContext, TaskType taskType) {
        if (taskType == null) {
            return null;
        }

        return switch (taskType) {
            case WUHuan -> fiveRingTaskV2Provider.getObject();
            case WUHuan_V2 -> fiveRingTaskV2Provider.getObject();
            case WUBEI -> wubeiTaskProvider.getObject();
            case XIULUO -> xiuluoTaskV2Provider.getObject();
            case XIULUO_V2 -> xiuluoTaskV2Provider.getObject();
            case AUTO_BATTLE -> autoBattleTaskProvider.getObject();
            case DEBUG_COORDINATE -> debugCoordinateTaskProvider.getObject();
            case DEBUG_MAP_CALIBRATOR -> debugMapCalibratorTaskProvider.getObject();
            case DEBUG_TEAM_ROLE -> debugTeamRoleTaskProvider.getObject();
            case DEBUG_XIULUO_STORY_OBJECTIVE -> debugXiuluoStoryObjectiveTaskProvider.getObject();
            case DEBUG_XIULUO_TASK_PANEL_OBJECTIVE -> debugXiuluoTaskPanelObjectiveTaskProvider.getObject();
            case DEBUG_XIULUO_MOCK_OBJECTIVE -> debugXiuluoMockObjectiveTaskProvider.getObject();
            case DEBUG_NAVIGATION_STRESS -> debugNavigationStressTaskProvider.getObject();
            case UNKNOWN -> null;
        };
    }
}
