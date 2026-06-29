package com.bot.dhxy.window.execution;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for runner route-dialog preparation timeout wiring.
 *
 * <p>The original 11:09/11:23 修罗 hang showed that a route OPTION prepare could enter OCR and never
 * return, freezing the watcher thread. This guard protects the intended shape: the watcher must call
 * route dialog preparation through a bounded future and fail closed on timeout instead of synchronously
 * blocking inside OCR.</p>
 */
public class RunnerRouteDialogPrepareTimeoutWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String runner = Files.readString(
                root.resolve("src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java"),
                StandardCharsets.UTF_8);

        require(runner.contains("WINDOW_ROUTE_DIALOG_PREPARE_TIMEOUT_MS"),
                "WindowTaskRunner must define a bounded route-dialog prepare timeout");
        require(runner.contains("dialogPreparationExecutor"),
                "route dialog OCR preparation must run outside the watcher executor");

        String refresh = between(runner,
                "private PreparedDialogAction refreshDialogPreparationSignal(",
                "private String resolveRoutePreparationTarget(");
        require(refresh.contains("runRouteDialogPreparationWithTimeout("),
                "refreshDialogPreparationSignal must call the timeout wrapper");
        require(!refresh.contains("? dialogService.prepareRememberedRouteOption(")
                        && !refresh.contains(": dialogService.prepareRouteKeywordOption("),
                "refreshDialogPreparationSignal must not synchronously call route dialog prepare");

        String wrapper = between(runner,
                "private Optional<PreparedDialogAction> runRouteDialogPreparationWithTimeout(",
                "private PreparedDialogAction bindAndPublishRouteDialogAction(");
        require(wrapper.contains("dialogPreparationExecutor.submit("),
                "timeout wrapper must submit route preparation to the dialog preparation executor");
        require(wrapper.contains(".get(WINDOW_ROUTE_DIALOG_PREPARE_TIMEOUT_MS, TimeUnit.MILLISECONDS)"),
                "timeout wrapper must bound waiting with WINDOW_ROUTE_DIALOG_PREPARE_TIMEOUT_MS");
        require(wrapper.contains("catch (TimeoutException"),
                "timeout wrapper must catch TimeoutException");
        require(wrapper.contains("future.cancel(true)"),
                "timeout wrapper must cancel timed-out preparation work");
        require(wrapper.contains("\"prepare-timeout\""),
                "timeout wrapper must log route dialog preparation as prepare-timeout");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex);
        if (endIndex < 0) {
            throw new AssertionError("Missing source marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
