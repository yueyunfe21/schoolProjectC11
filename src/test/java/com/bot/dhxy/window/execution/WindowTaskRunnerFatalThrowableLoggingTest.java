package com.bot.dhxy.window.execution;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for fatal task throwable diagnostics.
 *
 * <p>Live reproduction requires a task thread to hit a JVM/runtime fatal path. This guard protects
 * the runner boundary that must log non-Exception throwables before the task's default FAILED result
 * is emitted.</p>
 */
public class WindowTaskRunnerFatalThrowableLoggingTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String runner = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java"), StandardCharsets.UTF_8);

        String taskBoundary = between(runner,
                "private TaskRunResult runTaskWithBoundGameState(",
                "private CombatWatcherHandle startCombatWatcherIfNeeded(");
        require(taskBoundary.contains("} catch (Throwable e) {"),
                "runTaskWithBoundGameState must catch Throwable after Exception to log fatal task exits");
        require(taskBoundary.contains("task fatal throwable: "),
                "fatal throwable finish message must expose the throwable class");
        require(taskBoundary.contains("window [{}] task fatal error: {}"),
                "fatal throwable must be logged with stack trace at the task boundary");
        require(taskBoundary.indexOf("} catch (Exception e) {") < taskBoundary.indexOf("} catch (Throwable e) {"),
                "Throwable catch must stay after Exception so ordinary exceptions keep their existing logging path");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex + start.length());
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
