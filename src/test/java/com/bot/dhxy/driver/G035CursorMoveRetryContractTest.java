package com.bot.dhxy.driver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source contract for cursor-move convergence without weakening the final position proof.
 *
 * <p>G035 originally guarded the WinApi SetCursorPos retry; G146 deleted that backend, so the same
 * protection now anchors on the FakerInput absolute-move converge loop: every attempt must read the
 * cursor back, only a verified <=1px endpoint may return, corrections ride the same driver, and an
 * unreadable cursor fails closed.</p>
 */
public final class G035CursorMoveRetryContractTest {

    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/driver/fakerinput/FakerInputProvider.java"),
                StandardCharsets.UTF_8);
        String move = between(source,
                "private void moveToLogicalPoint(",
                "static int normalizeAbsoluteCoordinate(");
        require(move.contains("User32.INSTANCE.GetCursorPos(current)"),
                "every attempt must retain read-back verification");
        require(move.contains("Math.abs(current.x - targetX) <= 1")
                        && move.contains("Math.abs(current.y - targetY) <= 1"),
                "only a verified <=1px cursor position may return successfully");
        require(move.contains("device.updateRelativeMouse(heldMouseButtons, correctionX, correctionY"),
                "residual correction must ride the same driver, never SendInput/SetCursorPos");
        require(move.contains("throw new IllegalStateException(\"FakerInput could not read the current cursor position\")"),
                "an unreadable cursor must fail closed");
        require(!source.contains("SendInput") || source.contains("without reintroducing a SendInput"),
                "no SendInput fallback may reappear in the FakerInput provider");
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
