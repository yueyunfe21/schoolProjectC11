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
    private final ObjectProvider<SleepComputerTask> sleepComputerTaskProvider;

    public DefaultTaskFactory(ObjectProvider<FiveRingTaskV2> fiveRingTaskV2Provider,
                              ObjectProvider<WubeiTask> wubeiTaskProvider,
                              ObjectProvider<XiuluoTaskV2> xiuluoTaskV2Provider,
                              ObjectProvider<AutoBattleTask> autoBattleTaskProvider,
                              ObjectProvider<SleepComputerTask> sleepComputerTaskProvider) {
        this.fiveRingTaskV2Provider = fiveRingTaskV2Provider;
        this.wubeiTaskProvider = wubeiTaskProvider;
        this.xiuluoTaskV2Provider = xiuluoTaskV2Provider;
        this.autoBattleTaskProvider = autoBattleTaskProvider;
        this.sleepComputerTaskProvider = sleepComputerTaskProvider;
    }

    @Override
    public GameTask createTask(WindowRuntimeContext windowContext, TaskType taskType) {
        if (taskType == null) {
            return null;
        }

        return switch (taskType) {
            case WUHuan_V2 -> fiveRingTaskV2Provider.getObject();
            case WUBEI -> wubeiTaskProvider.getObject();
            case XIULUO -> xiuluoTaskV2Provider.getObject();
            case XIULUO_V2 -> xiuluoTaskV2Provider.getObject();
            case AUTO_BATTLE -> autoBattleTaskProvider.getObject();
            case SLEEP_COMPUTER -> sleepComputerTaskProvider.getObject();
            case UNKNOWN -> null;
        };
    }
}
