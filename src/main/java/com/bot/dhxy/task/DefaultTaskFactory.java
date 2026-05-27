package com.bot.dhxy.task;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.task.xiuluo.XiuluoTaskV2;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultTaskFactory implements TaskFactory {

    private final ObjectProvider<FiveRingTask> fiveRingTaskProvider;
    private final ObjectProvider<XiuluoTask> xiuluoTaskProvider;
    private final ObjectProvider<XiuluoTaskV2> xiuluoTaskV2Provider;
    private final ObjectProvider<AutoBattleTask> autoBattleTaskProvider;
    private final ObjectProvider<DebugCoordinateTask> debugCoordinateTaskProvider;
    private final ObjectProvider<DebugMapCalibratorTask> debugMapCalibratorTaskProvider;
    private final ObjectProvider<DebugTeamRoleTask> debugTeamRoleTaskProvider;
    private final ObjectProvider<DebugXiuluoStoryObjectiveTask> debugXiuluoStoryObjectiveTaskProvider;
    private final ObjectProvider<DebugXiuluoTaskPanelObjectiveTask> debugXiuluoTaskPanelObjectiveTaskProvider;
    private final ObjectProvider<DebugXiuluoMockObjectiveTask> debugXiuluoMockObjectiveTaskProvider;

    public DefaultTaskFactory(ObjectProvider<FiveRingTask> fiveRingTaskProvider,
                              ObjectProvider<XiuluoTask> xiuluoTaskProvider,
                              ObjectProvider<XiuluoTaskV2> xiuluoTaskV2Provider,
                              ObjectProvider<AutoBattleTask> autoBattleTaskProvider,
                              ObjectProvider<DebugCoordinateTask> debugCoordinateTaskProvider,
                              ObjectProvider<DebugMapCalibratorTask> debugMapCalibratorTaskProvider,
                              ObjectProvider<DebugTeamRoleTask> debugTeamRoleTaskProvider,
                              ObjectProvider<DebugXiuluoStoryObjectiveTask> debugXiuluoStoryObjectiveTaskProvider,
                              ObjectProvider<DebugXiuluoTaskPanelObjectiveTask> debugXiuluoTaskPanelObjectiveTaskProvider,
                              ObjectProvider<DebugXiuluoMockObjectiveTask> debugXiuluoMockObjectiveTaskProvider) {
        this.fiveRingTaskProvider = fiveRingTaskProvider;
        this.xiuluoTaskProvider = xiuluoTaskProvider;
        this.xiuluoTaskV2Provider = xiuluoTaskV2Provider;
        this.autoBattleTaskProvider = autoBattleTaskProvider;
        this.debugCoordinateTaskProvider = debugCoordinateTaskProvider;
        this.debugMapCalibratorTaskProvider = debugMapCalibratorTaskProvider;
        this.debugTeamRoleTaskProvider = debugTeamRoleTaskProvider;
        this.debugXiuluoStoryObjectiveTaskProvider = debugXiuluoStoryObjectiveTaskProvider;
        this.debugXiuluoTaskPanelObjectiveTaskProvider = debugXiuluoTaskPanelObjectiveTaskProvider;
        this.debugXiuluoMockObjectiveTaskProvider = debugXiuluoMockObjectiveTaskProvider;
    }

    @Override
    public GameTask createTask(WindowRuntimeContext windowContext, TaskType taskType) {
        if (taskType == null) {
            return null;
        }

        return switch (taskType) {
            case WUHuan -> fiveRingTaskProvider.getObject();
            case XIULUO -> xiuluoTaskProvider.getObject();
            case XIULUO_V2 -> xiuluoTaskV2Provider.getObject();
            case AUTO_BATTLE -> autoBattleTaskProvider.getObject();
            case DEBUG_COORDINATE -> debugCoordinateTaskProvider.getObject();
            case DEBUG_MAP_CALIBRATOR -> debugMapCalibratorTaskProvider.getObject();
            case DEBUG_TEAM_ROLE -> debugTeamRoleTaskProvider.getObject();
            case DEBUG_XIULUO_STORY_OBJECTIVE -> debugXiuluoStoryObjectiveTaskProvider.getObject();
            case DEBUG_XIULUO_TASK_PANEL_OBJECTIVE -> debugXiuluoTaskPanelObjectiveTaskProvider.getObject();
            case DEBUG_XIULUO_MOCK_OBJECTIVE -> debugXiuluoMockObjectiveTaskProvider.getObject();
            case UNKNOWN -> null;
        };
    }
}
