package com.bot.dhxy.window.interaction;

import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import org.springframework.stereotype.Service;

@Service
public class TaskWindowRuntimeService {

    private final TaskWindowBindingResolver bindingResolver;
    private final WindowFocusService windowFocusService;
    private final WindowNativeBindingRefreshService bindingRefreshService;

    public TaskWindowRuntimeService(TaskWindowBindingResolver bindingResolver,
                                    WindowFocusService windowFocusService,
                                    WindowNativeBindingRefreshService bindingRefreshService) {
        this.bindingResolver = bindingResolver;
        this.windowFocusService = windowFocusService;
        this.bindingRefreshService = bindingRefreshService;
    }

    public WindowNativeBinding binding(TaskExecutionContext context) {
        WindowNativeBinding binding = bindingResolver.resolve(context);
        return bindingRefreshService.refreshGeometry(binding).orElse(binding);
    }

    public boolean ready(TaskExecutionContext context) {
        WindowNativeBinding binding = binding(context);
        return binding.hasNativeHandle() && binding.hasGeometry();
    }

    public boolean activate(TaskExecutionContext context) {
        return windowFocusService.focus(binding(context));
    }

    public String describe(TaskExecutionContext context) {
        WindowNativeBinding binding = binding(context);
        return "windowId=" + (context == null ? "-" : context.getWindowId())
                + ", hwnd=" + (binding.getNativeHandle() == null ? "-" : binding.getNativeHandle())
                + ", title=" + binding.getTitle()
                + ", geometry=" + binding.getGeometryText()
                + ", ready=" + ready(context);
    }
}
