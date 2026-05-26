package com.bot.dhxy.task;

import com.bot.dhxy.model.TaskRunResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Placeholder for the future Zhuagui task in the multi-window task framework.
 */
@Slf4j
@Component
public class ZhuaguiTask implements GameTask {

    @Override
    public String getTaskCode() {
        return "zhuagui";
    }

    @Override
    public String getTaskName() {
        return "抓鬼";
    }

    @Override
    public TaskRunResult execute() {
        log.info("抓鬼任务已被调度，但当前还是占位版本，暂未实现具体逻辑。");
        return TaskRunResult.SKIPPED;
    }

    @Override
    public void stop() {
        log.info("收到停止抓鬼任务请求。");
    }
}
