package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Source contract proving G026's generic-close probe is exact-window read-only work. */
public final class G026ReadOnlyGenericCloseProbeContractTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String cleaner = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/service/UICleanerService.java"), StandardCharsets.UTF_8);
        String executor = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/cloud/turn/local/UiLocalOperationExecutor.java"), StandardCharsets.UTF_8);
        String dispatcher = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcher.java"), StandardCharsets.UTF_8);

        String probe = between(cleaner,
                "public Boolean probeGenericCloseButtonPresent(",
                "private boolean closeAllGenericWindows(");
        require(probe.contains("CleanupPass.start()") && probe.contains("cleanupPass.screenPath(tracker)"),
                "probe must capture the exact bound window once through CleanupPass");
        require(probe.contains("genericCloseButtonTemplates()"),
                "probe must scan the complete cancel-template catalog");
        require(probe.contains("ImageFinder.find(frame, template, 0.8)"),
                "probe must find every template against the same raw frame");
        require(!probe.contains("clickLeft") && !probe.contains("clickRight")
                        && !probe.contains("keyTap") && !probe.contains("submit"),
                "probe must never send input or acquire an input queue");
        require(executor.contains("case UI_PROBE_GENERIC_CLOSE -> executeProbeGenericClose(call)"),
                "typed operation must reach the read-only UI adapter");
        require(dispatcher.contains("UI_CLOSE_GENERIC_WINDOWS, UI_PROBE_GENERIC_CLOSE, UI_CLEAN_LIGHTWEIGHT"),
                "dispatcher must not wrap the read-only probe in an input callback");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = startIndex < 0 ? -1 : source.indexOf(end, startIndex);
        if (startIndex < 0 || endIndex < 0) {
            throw new AssertionError("missing source markers: " + start + " -> " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
