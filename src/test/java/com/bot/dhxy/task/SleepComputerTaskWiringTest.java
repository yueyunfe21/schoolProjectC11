package com.bot.dhxy.task;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for the UI task-queue sleep-computer action.
 */
public final class SleepComputerTaskWiringTest {

    private SleepComputerTaskWiringTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String taskType = read(root, "src/main/java/com/bot/dhxy/task/model/TaskType.java");
        String factory = read(root, "src/main/java/com/bot/dhxy/task/DefaultTaskFactory.java");
        String sleepTask = read(root, "src/main/java/com/bot/dhxy/task/SleepComputerTask.java");
        String powerService = read(root, "src/main/java/com/bot/dhxy/service/SystemPowerService.java");
        String ui = read(root, "src/main/java/com/bot/dhxy/ui/MainWindowController.java");

        require(taskType.contains("SLEEP_COMPUTER(\"sleep_computer\", \"睡眠计算机\")"),
                "TaskType must expose 睡眠计算机 as a queue-selectable task");
        require(factory.contains("ObjectProvider<SleepComputerTask>")
                        && factory.contains("case SLEEP_COMPUTER -> sleepComputerTaskProvider.getObject()"),
                "DefaultTaskFactory must create SleepComputerTask");
        require(sleepTask.contains("systemPowerService.sleepComputer(")
                        && sleepTask.contains("return TaskRunResult.SUCCESS"),
                "SleepComputerTask must delegate system sleep and report success after submission");
        require(powerService.contains("rundll32.exe")
                        && powerService.contains("powrprof.dll,SetSuspendState"),
                "SystemPowerService must use the Windows sleep command boundary");
        require(ui.contains("TaskType.SLEEP_COMPUTER")
                        && ui.contains("WindowTaskFailurePolicy.STOP_ON_FAILURE"),
                "UI queue submission must stop before sleep when an earlier queued task fails");
        require(ui.contains("case SLEEP_COMPUTER -> false"),
                "SleepComputer task must not expose a run-count editor");

        System.out.println("SleepComputerTaskWiringTest passed");
    }

    private static String read(Path root, String path) throws Exception {
        return Files.readString(root.resolve(path), StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
