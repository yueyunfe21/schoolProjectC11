package com.bot.dhxy.window.execution;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR66 WUBEI wake semantics.
 *
 * <p>Live validation needs a running 五倍 window, so this test protects the watcher wiring:
 * visible WUBEI dialogs may update snapshots and may produce explicit prepared-action events, but
 * they must not fall through to generic {@code TASK_ATTENTION_REQUIRED} wake publication.</p>
 */
public class WubeiPlainTaskAttentionNoWakeWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String runner = Files.readString(
                root.resolve("src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java"),
                StandardCharsets.UTF_8);

        String attention = between(runner,
                "private PreparedDialogAction publishTaskAttentionIfDialogVisible(",
                "private PreparedDialogAction refreshDialogPreparationSignal(");
        require(attention.contains("isWubeiDialogVisibleAttention(taskType)"),
                "WUBEI visible dialog branch must be handled before generic TASK_ATTENTION_REQUIRED publish");
        require(attention.indexOf("isWubeiDialogVisibleAttention(taskType)")
                        < attention.indexOf("windowReadyEventBus.publish(WindowReadyEvent.builder()"),
                "WUBEI no-generic-wake guard must run before generic TASK_ATTENTION_REQUIRED publish");

        String wubeiBranch = between(attention,
                "if (isWubeiDialogVisibleAttention(taskType))",
                "Optional<WindowReadyEvent> recent = windowReadyEventBus.latest(");
        require(!wubeiBranch.contains("WindowReadyEventType.TASK_ATTENTION_REQUIRED"),
                "WUBEI visible-dialog branch must not publish generic TASK_ATTENTION_REQUIRED");
        require(wubeiBranch.contains("refreshDialogPreparationSignal(")
                        && wubeiBranch.contains("refreshTaskDialogInterestPreparationSignal("),
                "WUBEI visible-dialog branch must still attempt route and task prepared-action preparation");
        require(wubeiBranch.contains("return preparedAction;"),
                "WUBEI branch must return explicit prepared action result without falling through");

        String helper = between(runner,
                "private boolean isWubeiDialogVisibleAttention(",
                "private PreparedDialogAction refreshDialogPreparationSignal(");
        require(helper.contains("taskType == TaskType.WUBEI"),
                "WUBEI helper must be scoped to TaskType.WUBEI only");
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
