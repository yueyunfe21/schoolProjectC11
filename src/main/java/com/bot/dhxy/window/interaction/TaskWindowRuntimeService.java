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

    public void clickCenter(TaskExecutionContext context) {
        windowInteractionService.clickCenter(binding(context));
    }

    public void clickRelative(TaskExecutionContext context, int relativeX, int relativeY) {
        windowInteractionService.clickRelative(binding(context), relativeX, relativeY);
    }

    public void moveRelative(TaskExecutionContext context, int relativeX, int relativeY) {
        windowInteractionService.moveRelative(binding(context), relativeX, relativeY);
    }

    public void activateAndClickCenter(TaskExecutionContext context) {
        windowInteractionService.focusAndClickCenter(binding(context));
    }

    public void activateAndClickRelative(TaskExecutionContext context, int relativeX, int relativeY) {
        windowInteractionService.focusAndClickRelative(binding(context), relativeX, relativeY);
    }

    public BufferedImage capture(TaskExecutionContext context) {
        return windowInteractionService.captureClientArea(binding(context));
    }

    public BufferedImage captureWindow(TaskExecutionContext context) {
        return windowInteractionService.captureWindow(binding(context));
    }

    public WindowRect client(TaskExecutionContext context) {
        return windowInteractionService.clientArea(binding(context));
    }

    public WindowRect window(TaskExecutionContext context) {
        return windowInteractionService.windowArea(binding(context));
    }

    public WindowPoint center(TaskExecutionContext context) {
        return windowInteractionService.center(binding(context));
    }

    public WindowPoint screenPoint(TaskExecutionContext context, int relativeX, int relativeY) {
        return windowInteractionService.screenPoint(binding(context), relativeX, relativeY);
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
