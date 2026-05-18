package com.bot.dhxy.window.interaction;

import com.bot.dhxy.runner.TaskExecutionContext;
import com.bot.dhxy.window.runtime.WindowNativeBinding;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

@Service
public class TaskWindowRuntimeService {

    private final TaskWindowBindingResolver bindingResolver;
    private final WindowInteractionService windowInteractionService;

    public TaskWindowRuntimeService(TaskWindowBindingResolver bindingResolver,
                                    WindowInteractionService windowInteractionService) {
        this.bindingResolver = bindingResolver;
        this.windowInteractionService = windowInteractionService;
    }

    public WindowNativeBinding binding(TaskExecutionContext context) {
        return bindingResolver.resolve(context);
    }

    public boolean ready(TaskExecutionContext context) {
        return bindingResolver.hasUsableWindow(context);
    }

    public boolean activate(TaskExecutionContext context) {
        return windowInteractionService.focus(binding(context));
    }

    public BufferedImage capture(TaskExecutionContext context) {
        return windowInteractionService.captureClientArea(binding(context));
    }

    public WindowRect client(TaskExecutionContext context) {
        return windowInteractionService.clientArea(binding(context));
    }
}
