package com.bot.dhxy.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 抓鬼任务占位模块。
 *
 * 现在先只注册任务，不写具体业务逻辑。
 * 目的：验证 TaskRunner / TaskQueue 能不能识别并执行多个任务。
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
    public void execute() {
        log.info("👻 抓鬼任务已被调度，但当前还是占位版本，暂未实现具体逻辑。");
    }

    @Override
    public void stop() {
        log.info("🛑 收到停止抓鬼任务请求。");
    }
}
