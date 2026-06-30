package com.bot.dhxy.window.execution;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR139 continuous cross-task startup reuse.
 *
 * <p>The live acceptance still needs a real `[五倍, 修罗]` / `[修罗, 五倍]` queue. This guard protects
 * the source wiring: a clean successful queued task handoff must carry an explicit startup mode,
 * reuse the same-queue common startup-prep marker, and skip only the next task's true-startup
 * hot-start/resume branch.</p>
 */
public class CR139CleanQueueTransitionStartupWiringTest {

    public static void main(String[] args) throws Exception {
        String runner = source("src", "main", "java", "com", "bot", "dhxy", "window", "execution",
                "WindowTaskRunner.java");
        String initializer = source("src", "main", "java", "com", "bot", "dhxy", "window", "execution",
                "DefaultWindowTaskStartupInitializer.java");
        String runtime = source("src", "main", "java", "com", "bot", "dhxy", "window", "runtime",
                "WindowRuntimeContext.java");
        String context = source("src", "main", "java", "com", "bot", "dhxy", "runner", "context",
                "TaskExecutionContext.java");
        String startupMode = source("src", "main", "java", "com", "bot", "dhxy", "runner", "context",
                "TaskStartupMode.java");
        String wubei = source("src", "main", "java", "com", "bot", "dhxy", "task", "wubei",
                "WubeiTask.java");
        String xiuluo = source("src", "main", "java", "com", "bot", "dhxy", "task", "xiuluo",
                "XiuluoTaskV2.java");

        require(startupMode.contains("CLEAN_QUEUE_TRANSITION"),
                "CR139 needs an explicit CLEAN_QUEUE_TRANSITION startup mode");
        require(context.contains("isCleanQueueTransitionStartup()"),
                "TaskExecutionContext must expose a clean queued transition predicate");

        String queueLoop = between(runner,
                "private void runQueueWithBoundGameState(",
                "private TaskStartupMode deferStartupIfAlreadyInCombat(");
        require(queueLoop.contains("TaskRunResult previousTaskResult = null"),
                "runner must remember the previous queued task result");
        require(queueLoop.contains("TaskType previousRequestedTaskType = TaskType.UNKNOWN"),
                "runner must remember the previous requested queued task");
        require(queueLoop.contains("startupMode = resolveCleanQueueTransitionStartupMode("),
                "runner must upgrade NORMAL startup into CLEAN_QUEUE_TRANSITION only after a clean queued handoff");
        require(queueLoop.contains("previousTaskResult = result"),
                "runner must update previous task result after each concrete task");
        require(queueLoop.contains("previousRequestedTaskType = requestedTaskType"),
                "runner must update previous requested task after each concrete task");

        String cleanHelper = between(runner,
                "private TaskStartupMode resolveCleanQueueTransitionStartupMode(",
                "private TaskStartupMode deferStartupIfAlreadyInCombat(");
        require(cleanHelper.contains("startupMode != TaskStartupMode.NORMAL"),
                "clean transition must not override after-combat startup recovery");
        require(cleanHelper.contains("previousTaskResult != TaskRunResult.SUCCESS"),
                "clean transition requires the previous queued task to finish successfully");
        require(cleanHelper.contains("previousRequestedTaskType == requestedTaskType"),
                "same-task continuation must not be treated as a cross-task transition");
        require(cleanHelper.contains("TaskStartupMode.CLEAN_QUEUE_TRANSITION"),
                "clean transition helper must return CLEAN_QUEUE_TRANSITION");

        require(runtime.contains("public boolean isTaskQueueStartupPreparationDone()"),
                "WindowRuntimeContext needs a queue-level common startup-prep marker predicate");
        String taskCodeMarker = between(runtime,
                "public boolean isTaskQueueStartupPreparationDone(String taskCode)",
                "public void markTaskQueueStartupPreparationDone(");
        require(taskCodeMarker.contains("return isTaskQueueStartupPreparationDone();"),
                "task-code startup marker check must delegate to the queue-level common marker");

        require(initializer.contains("executionContext.isCleanQueueTransitionStartup()"),
                "initializer must check the clean queued transition startup mode");
        require(initializer.contains("windowContext.isTaskQueueStartupPreparationDone()"),
                "initializer must reuse the queue-level common startup-prep marker");
        require(initializer.contains("startup init skipped: clean queued task transition reused common startup preparation"),
                "initializer skip log must make CR139 reuse obvious");
        require(initializer.contains("windowContext.markTaskQueueStartupPreparationDone(taskCode)"),
                "leader startup prep success must mark the same-queue common startup prep as done");

        String wubeiExecute = extractMethod(wubei,
                "public TaskRunResult execute(TaskExecutionContext executionContext)");
        require(wubeiExecute.contains("context.isCleanQueueTransitionStartup()"),
                "Wubei must read the clean queued transition startup predicate");
        require(wubeiExecute.contains("WubeiRoundContext.normalStart(round)"),
                "Wubei clean cross-task startup must enter normal accept flow instead of hot-start");
        require(wubeiExecute.contains("skip hot-start because clean queued task transition"),
                "Wubei must log that hot-start is skipped only for clean queued transition");

        String xiuluoExecute = extractMethod(xiuluo,
                "public TaskRunResult execute(TaskExecutionContext executionContext)");
        require(xiuluoExecute.contains("context.isCleanQueueTransitionStartup()"),
                "Xiuluo must read the clean queued transition startup predicate");
        require(xiuluoExecute.contains("cleanQueueTransitionStartup"),
                "Xiuluo must name the clean queued startup branch explicitly");
        require(xiuluoExecute.contains("skip startup-screen resume because clean queued task transition"),
                "Xiuluo must log that startup-screen resume is skipped only for clean queued transition");
        require(xiuluoExecute.contains("\"after-combat-exit-startup-screen-resume\""),
                "Xiuluo true after-combat startup recovery must remain available");

        System.out.println("CR139CleanQueueTransitionStartupWiringTest passed");
    }

    private static String source(String first, String... more) throws Exception {
        return Files.readString(Path.of(first, more), StandardCharsets.UTF_8);
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

    private static String extractMethod(String source, String signature) {
        int start = source.indexOf(signature);
        if (start < 0) {
            throw new AssertionError("Method signature not found: " + signature);
        }
        int brace = source.indexOf('{', start);
        if (brace < 0) {
            throw new AssertionError("Method body not found: " + signature);
        }
        int depth = 0;
        for (int i = brace; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(start, i + 1);
                }
            }
        }
        throw new AssertionError("Method body not closed: " + signature);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
