package com.bot.dhxy.window.execution;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR57 runner dialog detection reuse.
 *
 * <p>The production path depends on live game screenshots, so this guard checks the wiring shape:
 * a watcher tick must be able to pass one already-captured {@code DialogDetection} into route and
 * task dialog preparation instead of every branch unconditionally taking another screenshot.</p>
 */
public class RunnerDialogDetectionReuseWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String runner = read(root, "src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java");
        String dialogService = read(root, "src/main/java/com/bot/dhxy/service/DialogService.java");
        String provider = read(root, "src/main/java/com/bot/dhxy/window/dialog/WindowDialogPreparationProvider.java");
        String wubeiProvider = read(root, "src/main/java/com/bot/dhxy/task/wubei/WubeiDialogPreparationProvider.java");

        require(dialogService.contains("public DialogDetection detectDialogSnapshotNoFocus("),
                "DialogService must expose a no-focus DialogDetection capture for the watcher tick");
        require(dialogService.contains("prepareRouteKeywordOption(String source")
                        && dialogService.contains("String targetKeyword")
                        && dialogService.contains("DialogDetection suppliedDetection"),
                "route keyword prepare must accept a supplied DialogDetection");
        require(dialogService.contains("prepareRememberedChoiceOption(String source")
                        && dialogService.contains("DialogDetection suppliedDetection"),
                "remembered choice prepare must accept a supplied DialogDetection");
        require(dialogService.contains("prepareGreenTemplateOption(String source")
                        && dialogService.contains("DialogDetection suppliedDetection"),
                "green-template prepare must accept a supplied DialogDetection");
        require(dialogService.contains("prepareWhiteStoryTemplateOrAbsent(String source")
                        && dialogService.contains("DialogDetection suppliedDetection"),
                "white-story prepare must accept a supplied DialogDetection");

        require(provider.contains("prepare(WindowDialogInterest interest")
                        && provider.contains("DialogDetection suppliedDetection"),
                "WindowDialogPreparationProvider must allow runner to pass the tick detection");
        require(wubeiProvider.contains("prepare(interest, operation, source, null)")
                        && wubeiProvider.contains("DialogDetection suppliedDetection"),
                "Wubei provider must preserve old callers and use the supplied detection in the new path");

        require(runner.contains("TickDialogProbe"),
                "WindowTaskRunner must have a tick-scoped dialog probe/carrier");
        require(runner.contains("publishTaskAttentionIfDialogVisible(")
                        && runner.contains("tickDialogProbe"),
                "task attention must receive the same tick dialog probe");
        require(runner.contains("refreshDialogPreparationSignal(")
                        && runner.contains("refreshTaskDialogInterestPreparationSignal(")
                        && runner.contains("TickDialogProbe tickDialogProbe"),
                "route and task dialog preparation must receive the tick dialog probe");
    }

    private static String read(Path root, String relativePath) throws Exception {
        return Files.readString(root.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
