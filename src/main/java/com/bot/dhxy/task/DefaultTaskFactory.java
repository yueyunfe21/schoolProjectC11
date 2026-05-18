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

    public DefaultTaskFactory(ObjectProvider<FiveRingTask> fiveRingTaskProvider) {
        this.fiveRingTaskProvider = fiveRingTaskProvider;
    }

    @Override
    public GameTask createTask(WindowRuntimeContext windowContext, TaskType taskType) {
        if (taskType == null) {
            return null;
        }

        return switch (taskType) {
            case WUHuan -> {
                /*
                 * This now asks Spring for a fresh FiveRingTask instance.
                 *
                 * Remaining multi-window limitation:
                 * FiveRingTask still depends on the current shared GameContext/service graph.
                 * Real per-window execution still needs window-scoped GameContext/services.
                 */
                yield fiveRingTaskProvider.getObject();
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
