package com.bot.dhxy.window.execution;

import java.nio.file.Files;
import java.nio.file.Path;

public final class XiuluoEnterBattleInterestPriorityWiringTest {
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java"));
        String attention = between(source,
                "private PreparedDialogAction publishTaskAttentionIfDialogVisible",
                "private boolean isWubeiDialogVisibleAttention");
        String taskInterest = between(source,
                "private PreparedDialogAction refreshTaskDialogInterestPreparationSignal",
                "private boolean canPrepareTaskDialogWithoutVisibleSnapshot");

        require(source.contains("shouldPrioritizeTaskDialogInterest(taskType, visibleType)"),
                "runner must have a task-interest priority gate for visible option dialogs");
        String genericFollowUp = attention.substring(attention.indexOf(
                "If this visible option belongs to the active route intent"));
        int priorityPrepare = genericFollowUp.indexOf("shouldPrioritizeTaskDialogInterest(taskType, visibleType)");
        int routePrepare = genericFollowUp.indexOf("refreshDialogPreparationSignal(");
        require(priorityPrepare >= 0 && routePrepare >= 0 && priorityPrepare < routePrepare,
                "XIULUO_ENTER_BATTLE interest must be prepared before route transfer preparation");
        require(source.contains("TaskType.XIULUO_V2")
                        && source.contains("DialogOperation.XIULUO_ENTER_BATTLE")
                        && source.contains("visibleType == DialogType.OPTION"),
                "priority gate must be restricted to visible OPTION + XIULUO_ENTER_BATTLE");
        require(taskInterest.contains("shouldClearPreparedActionForTaskInterest("),
                "fresh ROUTE_TRANSFER prepared actions must not block XIULUO_ENTER_BATTLE interest");
        require(taskInterest.contains("task dialog interest overrides existing prepared action"),
                "existing mismatched prepared action should be cleared with an explicit reason");
    }

    private static String between(String text, String start, String end) {
        int startIndex = text.indexOf(start);
        int endIndex = text.indexOf(end, startIndex);
        if (startIndex < 0 || endIndex < 0) {
            throw new AssertionError("Could not find source slice");
        }
        return text.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
