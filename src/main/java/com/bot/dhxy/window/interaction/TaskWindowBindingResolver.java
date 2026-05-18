package com.bot.dhxy.window.interaction;

import com.bot.dhxy.runner.TaskExecutionContext;
import com.bot.dhxy.window.runtime.WindowNativeBinding;
import org.springframework.stereotype.Component;

/**
 * 从 TaskExecutionContext 里还原当前任务对应的真实窗口绑定信息。
 */
@Component
public class TaskWindowBindingResolver {

    public WindowNativeBinding resolve(TaskExecutionContext context) {
        if (context == null || !context.hasNativeWindow()) {
            return WindowNativeBinding.empty();
        }
        return new WindowNativeBinding(
                context.getNativeWindowHandle(),
                context.getNativeWindowTitle(),
                context.getNativeWindowClassName(),
                context.getNativeWindowProcessId(),
                context.getNativeWindowX(),
                context.getNativeWindowY(),
                context.getNativeWindowWidth(),
                context.getNativeWindowHeight()
        );
    }

    public boolean hasUsableWindow(TaskExecutionContext context) {
        WindowNativeBinding binding = resolve(context);
        return binding.hasNativeHandle() && binding.hasGeometry();
    }
}
