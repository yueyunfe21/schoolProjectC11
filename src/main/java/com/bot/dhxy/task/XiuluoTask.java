package com.bot.dhxy.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 修罗任务占位模块。
 *
 * 现在先只注册任务，不写具体业务逻辑。
 * 目的：先让任务平台具备多个可勾选任务，后面再逐步补充修罗流程。
 */
@Slf4j
@Component
public class XiuluoTask implements GameTask {

    @Override
    public String getTaskCode() {
        return "xiuluo";
    }

    @Override
    public String getTaskName() {
        return "修罗";
    }

    @Override
    public void execute() {
        log.info("⚔️ 修罗任务已被调度，但当前还是占位版本，暂未实现具体逻辑。");
    }

    @Override
    public void stop() {
        log.info("🛑 收到停止修罗任务请求。");
    }
}
