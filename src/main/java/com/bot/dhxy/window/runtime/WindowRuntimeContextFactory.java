package com.bot.dhxy.window.runtime;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.window.model.WindowNativeBinding;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 创建窗口级运行上下文。
 *
 * 多窗口目前共用同一个 GameContext Bean，但每个 WindowRuntimeContext 内部持有独立 GameContext.State。
 */
@Component
public class WindowRuntimeContextFactory {

    private final ObjectProvider<GameContext> gameContextProvider;

    public WindowRuntimeContextFactory(ObjectProvider<GameContext> gameContextProvider) {
        this.gameContextProvider = gameContextProvider;
    }

    public WindowRuntimeContext create(WindowRegistrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("window registration request must not be null");
        }
        request.requireValid();

        WindowRuntimeContext context = new WindowRuntimeContext(request.getWindowId(), gameContextProvider.getObject());
        context.applyRegistration(request, true);
        return context;
    }
}
