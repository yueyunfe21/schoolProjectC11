package com.bot.dhxy.task;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultTaskFactory implements TaskFactory {

    private final ObjectProvider<FiveRingTask> fiveRingTaskProvider;
    private final ObjectProvider<AutoBattleTask> autoBattleTaskProvider;

    public DefaultTaskFactory(ObjectProvider<FiveRingTask> fiveRingTaskProvider,
                              ObjectProvider<AutoBattleTask> autoBattleTaskProvider) {
        this.fiveRingTaskProvider = fiveRingTaskProvider;
        this.autoBattleTaskProvider = autoBattleTaskProvider;
    }

    @Override
    public GameTask createTask(WindowRuntimeContext windowContext, TaskType taskType) {
        if (taskType == null) {
            return null;
        }

        return switch (taskType) {
            case WUHuan -> fiveRingTaskProvider.getObject();
            case AUTO_BATTLE -> autoBattleTaskProvider.getObject();
            case UNKNOWN -> null;
        };
    }
}
