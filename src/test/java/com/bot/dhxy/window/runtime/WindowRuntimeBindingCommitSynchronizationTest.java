package com.bot.dhxy.window.runtime;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level concurrency guard for CR95 native-binding commits.
 */
public class WindowRuntimeBindingCommitSynchronizationTest {

    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of("src", "main", "java", "com", "bot", "dhxy",
                "window", "runtime", "WindowRuntimeContext.java"), StandardCharsets.UTF_8);
        String refreshService = Files.readString(Path.of("src", "main", "java", "com", "bot", "dhxy",
                "window", "runtime", "WindowNativeBindingRefreshService.java"), StandardCharsets.UTF_8);
        String tracker = Files.readString(Path.of("src", "main", "java", "com", "bot", "dhxy",
                "core", "GameClientTracker.java"), StandardCharsets.UTF_8);
        String manager = Files.readString(Path.of("src", "main", "java", "com", "bot", "dhxy",
                "window", "execution", "MultiWindowTaskManager.java"), StandardCharsets.UTF_8);
        String taskWindow = Files.readString(Path.of("src", "main", "java", "com", "bot", "dhxy",
                "window", "interaction", "TaskWindowRuntimeService.java"), StandardCharsets.UTF_8);

        assertContains(source, "public synchronized WindowIdentityDrift setNativeBinding");
        assertOrder(source, "WindowNativeBinding previous = this.nativeBinding;", "this.nativeBinding = next;");
        assertOrder(source, "this.nativeBinding = next;", "return drift;");
        assertContains(refreshService, "public Optional<WindowNativeBinding> refreshAndCommit(WindowRuntimeContext context)");
        assertOrder(refreshService, "synchronized (context)", "Optional<WindowNativeBinding> refreshed = refreshGeometry(binding);");
        assertOrder(refreshService, "Optional<WindowNativeBinding> refreshed = refreshGeometry(binding);",
                "refreshed.ifPresent(context::setNativeBinding);");
        assertContains(tracker, "bindingRefreshService.refreshAndCommit(context)");
        assertContains(manager, "bindingRefreshService.refreshAndCommit(runner.getWindowContext())");
        assertContains(taskWindow, "bindingRefreshService.refreshAndCommit(runtime.get())");

        System.out.println("WindowRuntimeBindingCommitSynchronizationTest passed");
    }

    private static void assertContains(String value, String token) {
        if (!value.contains(token)) {
            throw new AssertionError("Expected token missing: " + token);
        }
    }

    private static void assertOrder(String value, String firstToken, String secondToken) {
        int first = value.indexOf(firstToken);
        int second = value.indexOf(secondToken);
        if (first < 0 || second < 0 || first >= second) {
            throw new AssertionError("Expected token order missing: " + firstToken + " before " + secondToken);
        }
    }
}
