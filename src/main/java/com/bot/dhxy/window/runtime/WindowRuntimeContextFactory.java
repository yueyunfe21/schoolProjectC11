package com.bot.dhxy.window.runtime;

import com.bot.dhxy.core.GameContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 创建窗口级运行上下文。
 *
 * 目前 GameContext 仍是全局兼容模式；后续真正多窗口时，应通过这里切换为窗口级 GameContext。
 */
@Component
public class WindowRuntimeContextFactory {

    private final ObjectProvider<GameContext> gameContextProvider;

    public WindowRuntimeContextFactory(ObjectProvider<GameContext> gameContextProvider) {
        this.gameContextProvider = gameContextProvider;
    }

    public WindowRuntimeContext create(WindowRegistrationRequest request) {
        WindowRuntimeContext context = new WindowRuntimeContext(request.getWindowId(), gameContextProvider.getObject());
        context.updateRole(request.getRole(), request.getRoleName());
        context.setSelectedTaskType(request.getSelectedTaskType());
        return context;
    }
}
