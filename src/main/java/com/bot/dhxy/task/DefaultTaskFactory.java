package com.bot.dhxy.task;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultTaskFactory implements TaskFactory {

    private final FiveRingTask fiveRingTask;

    public DefaultTaskFactory(FiveRingTask fiveRingTask) {
        this.fiveRingTask = fiveRingTask;
    }

    @Override
    public GameTask createTask(WindowRuntimeContext windowContext, TaskType taskType) {
        if (taskType == null) {
            return null;
        }

        return switch (taskType) {
            case WUHuan -> {
                /*
                 * Temporary compatibility path: FiveRingTask is still a Spring singleton
                 * with a shared GameContext and service graph. Real multi-window execution
                 * must create a window-scoped task instance from WindowRuntimeContext
                 * instead of sharing this singleton across windows.
                 */
                yield fiveRingTask;
            }
            case AUTO_BATTLE -> {
                log.warn("AUTO_BATTLE task creation is not implemented yet for window [{}]",
                        windowContext == null ? null : windowContext.getWindowId());
                yield null;
            }
            case UNKNOWN -> null;
        };
    }
}
