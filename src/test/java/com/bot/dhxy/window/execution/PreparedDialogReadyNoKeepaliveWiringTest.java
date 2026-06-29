package com.bot.dhxy.window.execution;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR58 prepared-action lifecycle wiring.
 *
 * <p>Live validation requires a game dialog screenshot, so this test protects the wiring shape:
 * once a prepared action is READY, the watcher must stop doing background fingerprint keepalive.
 * Fingerprint validation belongs to the atomic consume path, immediately before a click-required
 * action is consumed.</p>
 */
public class PreparedDialogReadyNoKeepaliveWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String runner = read(root, "src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java");
        String runtime = read(root, "src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java");
        String dialogService = read(root, "src/main/java/com/bot/dhxy/service/DialogService.java");

        String observerIntervalBlock = between(runner,
                "long intervalMs = WINDOW_COMBAT_GUARD_IDLE_INTERVAL_MS;",
                "logSlowObserverTick(");
        require(!observerIntervalBlock.contains("|| preparedDialogAction != null"),
                "preparedDialogAction alone must not force the observer into hot dialog cadence");

        String routeRefresh = between(runner,
                "private PreparedDialogAction refreshDialogPreparationSignal(",
                "private String resolveRoutePreparationTarget(");
        require(!routeRefresh.contains("validatePreparedDialogAction("),
                "route refresh must not fingerprint-revalidate an unchanged prepared action");

        String taskRefresh = between(runner,
                "private PreparedDialogAction refreshTaskDialogInterestPreparationSignal(",
                "private boolean canPrepareTaskDialogWithoutVisibleSnapshot(");
        require(!taskRefresh.contains("validatePreparedDialogAction("),
                "task dialog interest refresh must not fingerprint-revalidate an unchanged prepared action");

        String trackerRefresh = between(runner,
                "private PreparedDialogAction refreshTaskTrackerPreparationSignal(",
                "private boolean hasTaskDialogInterest(");
        require(!trackerRefresh.contains("validatePreparedDialogAction("),
                "task tracker refresh must not fingerprint-revalidate an unchanged prepared action");

        require(runtime.contains("interface PreparedDialogActionValidator"),
                "WindowRuntimeContext must expose a validation callback for atomic consume");
        require(runtime.contains("consumePreparedDialogActionValidated("),
                "WindowRuntimeContext must provide validation-aware prepared-action consume");
        require(dialogService.contains("validatePreparedDialogActionForConsume("),
                "DialogService must expose the consume-time fingerprint validation helper");
    }

    private static String read(Path root, String relativePath) throws Exception {
        return Files.readString(root.resolve(relativePath), StandardCharsets.UTF_8);
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
