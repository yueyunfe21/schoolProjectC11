package com.bot.dhxy.window.execution;

import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.policy.WindowCapacityPolicy;
import com.bot.dhxy.window.runtime.WindowHandleParser;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowRuntimeContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Registry of exact window runtime handles used by the HTTPS turn client. */
@Component
public class MultiWindowTaskManager {

    private final WindowRuntimeContextFactory windowRuntimeContextFactory;
    private final WindowCapacityPolicy windowCapacityPolicy;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final Map<String, WindowTaskRunner> runnersByWindowId = new ConcurrentHashMap<>();

    @Autowired
    public MultiWindowTaskManager(WindowRuntimeContextFactory windowRuntimeContextFactory,
                                  WindowCapacityPolicy windowCapacityPolicy,
                                  WindowNativeBindingRefreshService bindingRefreshService) {
        this.windowRuntimeContextFactory = windowRuntimeContextFactory;
        this.windowCapacityPolicy = windowCapacityPolicy;
        this.bindingRefreshService = bindingRefreshService;
    }

    public WindowTaskRunner registerWindow(WindowRegistrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("window registration request must not be null");
        }
        request.requireValid();
        return runnersByWindowId.compute(request.getWindowId(), (windowId, existing) -> {
            if (existing != null) {
                existing.refreshRegistration(request);
                return existing;
            }
            if (!windowCapacityPolicy.canRegister(runnersByWindowId.size())) {
                return null;
            }
            return new WindowTaskRunner(windowRuntimeContextFactory.create(request));
        });
    }

    public WindowTaskRunner registerWindow(WindowRuntimeContext windowContext) {
        if (windowContext == null) {
            throw new IllegalArgumentException("window context must not be null");
        }
        WindowTaskRunner existing = runnersByWindowId.get(windowContext.getWindowId());
        if (existing != null) {
            return existing;
        }
        if (!windowCapacityPolicy.canRegister(runnersByWindowId.size())) {
            return null;
        }
        return runnersByWindowId.computeIfAbsent(
                windowContext.getWindowId(), ignored -> new WindowTaskRunner(windowContext));
    }

    public int registerWindows(Collection<WindowRegistrationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return 0;
        }
        int registered = 0;
        for (WindowRegistrationRequest request : requests) {
            if (request != null && request.hasWindowId() && registerWindow(request) != null) {
                registered++;
            }
        }
        return registered;
    }

    public Optional<WindowTaskRunner> getRunner(String windowId) {
        return Optional.ofNullable(runnersByWindowId.get(normalizeWindowId(windowId)));
    }

    public Optional<WindowTaskSnapshot> getSnapshot(String windowId) {
        return getRunner(windowId).map(WindowTaskRunner::snapshot);
    }

    public List<WindowTaskSnapshot> getAllSnapshots() {
        return runnersByWindowId.values().stream()
                .map(WindowTaskRunner::snapshot)
                .sorted(Comparator.comparing(
                        WindowTaskSnapshot::getWindowId, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    public Collection<WindowTaskRunner> getAllRunners() {
        return Collections.unmodifiableCollection(runnersByWindowId.values());
    }

    public int getRegisteredWindowCount() {
        return runnersByWindowId.size();
    }

    public int getRunningWindowCount() {
        return (int) runnersByWindowId.values().stream().filter(WindowTaskRunner::isRunning).count();
    }

    public boolean hasRunningTasks() {
        return getRunningWindowCount() > 0;
    }

    public int getMaxWindowCount() {
        return windowCapacityPolicy.getMaxWindowCount();
    }

    public int getRemainingWindowCapacity() {
        return windowCapacityPolicy.remainingCapacity(runnersByWindowId.size());
    }

    public void unregisterWindow(String windowId) {
        WindowTaskRunner runner = runnersByWindowId.remove(normalizeWindowId(windowId));
        if (runner != null) {
            runner.shutdownNow();
        }
    }

    public void unregisterAll() {
        List.copyOf(runnersByWindowId.keySet()).forEach(this::unregisterWindow);
    }

    private boolean refreshLiveNativeBinding(WindowTaskRunner runner) {
        if (runner == null || runner.getWindowContext() == null) {
            return false;
        }
        WindowNativeBinding binding = runner.getWindowContext().getNativeBinding();
        if (binding == null || !binding.hasNativeHandle()) {
            return true;
        }
        if (WindowHandleParser.parseHandle(binding.getNativeHandle()) == null) {
            return false;
        }
        return bindingRefreshService.refreshAndCommit(runner.getWindowContext()).isPresent();
    }

    private String normalizeWindowId(String windowId) {
        if (windowId == null) {
            return null;
        }
        String trimmed = windowId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
