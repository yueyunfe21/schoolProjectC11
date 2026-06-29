package com.bot.dhxy.window.interaction;

import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TaskWindowRuntimeService {

    private final TaskWindowBindingResolver bindingResolver;
    private final WindowFocusService windowFocusService;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final WindowTaskContextHolder windowTaskContextHolder;

    public TaskWindowRuntimeService(TaskWindowBindingResolver bindingResolver,
                                    WindowFocusService windowFocusService,
                                    WindowNativeBindingRefreshService bindingRefreshService,
                                    WindowTaskContextHolder windowTaskContextHolder) {
        this.bindingResolver = bindingResolver;
        this.windowFocusService = windowFocusService;
        this.bindingRefreshService = bindingRefreshService;
        this.windowTaskContextHolder = windowTaskContextHolder;
    }

    public WindowNativeBinding binding(TaskExecutionContext context) {
        WindowNativeBinding binding = bindingResolver.resolve(context);
        Optional<WindowRuntimeContext> runtime = windowTaskContextHolder.rawCurrent()
                .filter(current -> sameRuntimeWindow(context, current));
        if (runtime.isPresent()) {
            return bindingRefreshService.refreshAndCommit(runtime.get())
                    .orElse(runtime.get().getNativeBinding());
        }
        return bindingRefreshService.refreshGeometry(binding).orElse(binding);
    }

    private boolean sameRuntimeWindow(TaskExecutionContext context, WindowRuntimeContext runtime) {
        if (runtime == null) {
            return false;
        }
        if (context == null || !context.hasWindow()) {
            return true;
        }
        return context.getWindowId().equals(runtime.getWindowId());
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
