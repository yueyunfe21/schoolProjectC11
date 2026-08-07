package com.bot.dhxy.driver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Source contract for transient SetCursorPos retry without weakening the final position proof. */
public final class G035CursorMoveRetryContractTest {

    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/driver/WinApiMouseController.java"), StandardCharsets.UTF_8);
        String move = between(source,
                "private void moveCursorToLogicalPoint(",
                "private static INPUT buildMouseInput(");
        require(source.contains("CURSOR_MOVE_MAX_ATTEMPTS = 3"),
                "cursor movement must have a bounded three-attempt policy");
        require(move.contains("attempt <= CURSOR_MOVE_MAX_ATTEMPTS"),
                "SetCursorPos retry must stay inside the existing move method");
        require(move.contains("User32.INSTANCE.SetCursorPos(physicalX, physicalY)"),
                "every attempt must use the same physical target");
        require(move.contains("User32.INSTANCE.GetCursorPos(after)"),
                "every attempt must retain read-back verification");
        require(move.contains("if (reached) {\n                return;"),
                "only a verified cursor position may return successfully");
        require(move.indexOf("throw new IllegalStateException") > move.indexOf("for (int attempt"),
                "exhausted retries must still fail closed");
        System.out.println("G035_CURSOR_MOVE_RETRY_CONTRACT_PASS=1/1");
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
