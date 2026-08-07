package com.bot.dhxy.driver;

import com.bot.dhxy.runner.stop.TaskSleep;

import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.WindowAwareInputCoordinator;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowHandleParser;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.INPUT;
import com.sun.jna.platform.win32.WinDef.POINT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;

@Component
@ConditionalOnProperty(prefix = "bot.input", name = "backend", havingValue = "WIN_API", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class WinApiMouseController implements InputProvider {

    private static final int FLAG_MOUSE_LEFT_DOWN = 0x0002;
    private static final int FLAG_MOUSE_LEFT_UP = 0x0004;
    private static final int FLAG_MOUSE_RIGHT_DOWN = 0x0008;
    private static final int FLAG_MOUSE_RIGHT_UP = 0x0010;
    private static final int FLAG_MOUSE_WHEEL = 0x0800;

    private static final int FLAG_KEY_UP = 0x0002;
    private static final int FLAG_KEY_SCANCODE = 0x0008;
    private static final int FLAG_KEY_UNICODE = 0x0004;

    private static final int VK_CONTROL = 0x11;
    private static final int VK_V = 0x56;

    private static final int SCAN_LALT = 0x38;
    private static final int SCAN_LCTRL = 0x1D;
    private static final int SCAN_1 = 0x02;
    private static final int SCAN_2 = 0x03;
    private static final int SCAN_4 = 0x05;
    private static final int SCAN_5 = 0x06;
    private static final int SCAN_6 = 0x07;
    private static final int SCAN_8 = 0x09;
    private static final int SCAN_A = 0x1E;
    private static final int SCAN_B = 0x30;
    private static final int SCAN_C = 0x2E;
    private static final int SCAN_E = 0x12;
    private static final int SCAN_O = 0x18;
    private static final int SCAN_Q = 0x10;
    private static final int SCAN_T = 0x14;
    private static final int SCAN_U = 0x16;
    private static final int SCAN_ENTER = 0x1C;
    private static final int SCAN_ESCAPE = 0x01;

    private static final String UNION_FIELD_MOUSE = "mi";
    private static final String UNION_FIELD_KEYBOARD = "ki";
    private static final DWORD INPUT_ARRAY_SIZE_ONE = new DWORD(1);
    private static final int LEFT_CLICK_HOLD_MS = 150;
    private static final int CURSOR_MOVE_MAX_ATTEMPTS = 3;
    private static final int CURSOR_MOVE_RETRY_DELAY_MS = 50;

    private final CoordinateHelper coordinateHelper;
    private final WindowAwareInputCoordinator inputCoordinator;
    private final WindowTaskContextHolder windowTaskContextHolder;

    @Override
    public void clickLeft(int x, int y, int delayMs) {
        traceInput("clickLeft", "button=LEFT x=" + x + " y=" + y
                + " requestedDelayMs=" + delayMs + " effectiveDelayMs=" + LEFT_CLICK_HOLD_MS);
        inputCoordinator.runInput("clickLeft", () -> {
            logClickForeground("before", x, y);
            doClick(x, y, LEFT_CLICK_HOLD_MS, FLAG_MOUSE_LEFT_DOWN, FLAG_MOUSE_LEFT_UP);
            logClickForeground("after", x, y);
        });
    }

    @Override
    public void clickRight(int x, int y, int delayMs) {
        traceInput("clickRight", "button=RIGHT x=" + x + " y=" + y + " delayMs=" + delayMs);
        inputCoordinator.runInput("clickRight", () -> doClick(x, y, delayMs, FLAG_MOUSE_RIGHT_DOWN, FLAG_MOUSE_RIGHT_UP));
    }

    @Override
    public void doubleRightClick(int x, int y, int clickDelayMs, int intervalMs) {
        traceInput("doubleRightClick", "button=RIGHT x=" + x + " y=" + y
                + " clickDelayMs=" + clickDelayMs + " intervalMs=" + intervalMs);
        inputCoordinator.runInput("doubleRightClick", () -> {
            doClick(x, y, clickDelayMs, FLAG_MOUSE_RIGHT_DOWN, FLAG_MOUSE_RIGHT_UP);
            TaskSleep.sleep(intervalMs);
            doClick(x, y, clickDelayMs, FLAG_MOUSE_RIGHT_DOWN, FLAG_MOUSE_RIGHT_UP);
        });
    }

    @Override
    public void moveMouse(int x, int y) {
        traceInput("moveMouse", "x=" + x + " y=" + y);
        inputCoordinator.runInput("moveMouse", () -> moveCursorToLogicalPoint(x, y));
    }

    @Override
    public void holdCtrl() {
        traceInput("holdCtrl", "");
        inputCoordinator.runInput("holdCtrl", this::doHoldCtrl);
    }

    @Override
    public void releaseCtrl() {
        traceInput("releaseCtrl", "");
        inputCoordinator.runInput("releaseCtrl", this::doReleaseCtrl);
    }

    @Override
    public void pressCtrlU() {
        traceInput("pressCtrlU", "shortcut=CTRL+U");
        inputCoordinator.runInput("pressCtrlU", () -> pressCtrlScan(SCAN_U, "CTRL+U"));
    }

    @Override
    public void pressCtrlA() {
        traceInput("pressCtrlA", "shortcut=CTRL+A");
        inputCoordinator.runInput("pressCtrlA", () -> pressCtrlScan(SCAN_A, "CTRL+A"));
    }

    @Override
    public void pressAlt1() {
        traceInput("pressAlt1", "shortcut=ALT+1");
        inputCoordinator.runInput("pressAlt1", () -> pressAltScan(SCAN_1, "ALT+1"));
    }

    @Override
    public void pressAlt2() {
        traceInput("pressAlt2", "shortcut=ALT+2");
        inputCoordinator.runInput("pressAlt2", () -> pressAltScan(SCAN_2, "ALT+2"));
    }

    @Override
    public void pressAlt4() {
        traceInput("pressAlt4", "shortcut=ALT+4");
        inputCoordinator.runInput("pressAlt4", () -> pressAltScan(SCAN_4, "ALT+4"));
    }

    @Override
    public void pressAlt5() {
        traceInput("pressAlt5", "shortcut=ALT+5");
        inputCoordinator.runInput("pressAlt5", () -> pressAltScan(SCAN_5, "ALT+5"));
    }

    @Override
    public void pressAlt6() {
        traceInput("pressAlt6", "shortcut=ALT+6");
        inputCoordinator.runInput("pressAlt6", () -> pressAltScan(SCAN_6, "ALT+6"));
    }

    @Override
    public void pressAlt8() {
        traceInput("pressAlt8", "shortcut=ALT+8");
        inputCoordinator.runInput("pressAlt8", () -> pressAltScan(SCAN_8, "ALT+8"));
    }

    @Override
    public void pressAltT() {
        traceInput("pressAltT", "shortcut=ALT+T");
        inputCoordinator.runInput("pressAltT", () -> pressAltScan(SCAN_T, "ALT+T"));
    }

    @Override
    public void pressAltU() {
        traceInput("pressAltU", "shortcut=ALT+U");
        inputCoordinator.runInput("pressAltU", () -> pressAltScan(SCAN_U, "ALT+U"));
    }

    @Override
    public void pressAltO() {
        traceInput("pressAltO", "shortcut=ALT+O");
        inputCoordinator.runInput("pressAltO", () -> pressAltScan(SCAN_O, "ALT+O"));
    }

    @Override
    public void pressAltE() {
        traceInput("pressAltE", "shortcut=ALT+E");
        inputCoordinator.runInput("pressAltE", () -> pressAltScan(SCAN_E, "ALT+E"));
    }

    @Override
    public void pressAltQ() {
        traceInput("pressAltQ", "shortcut=ALT+Q");
        inputCoordinator.runInput("pressAltQ", () -> pressAltScan(SCAN_Q, "ALT+Q"));
    }

    @Override
    public void pressAltA() {
        traceInput("pressAltA", "shortcut=ALT+A");
        inputCoordinator.runInput("pressAltA", () -> pressAltScan(SCAN_A, "ALT+A"));
    }

    @Override
    public void pressAltB() {
        traceInput("pressAltB", "shortcut=ALT+B");
        inputCoordinator.runInput("pressAltB", () -> pressAltScan(SCAN_B, "ALT+B"));
    }

    @Override
    public void pressAltC() {
        traceInput("pressAltC", "shortcut=ALT+C");
        inputCoordinator.runInput("pressAltC", () -> pressAltScan(SCAN_C, "ALT+C"));
    }

    @Override
    public void pressEnter() {
        traceInput("pressEnter", "key=ENTER");
        inputCoordinator.runInput("pressEnter", this::doPressEnter);
    }

    @Override
    public void pressEscape() {
        traceInput("pressEscape", "key=ESCAPE");
        inputCoordinator.runInput("pressEscape", () -> {
            sendInput(buildKeyboardScanInput(SCAN_ESCAPE, false));
            TaskSleep.sleep(60);
            sendInput(buildKeyboardScanInput(SCAN_ESCAPE, true));
            TaskSleep.sleep(60);
        });
    }

    @Override
    public void pasteText(String text) {
        if (text == null) {
            return;
        }
        traceInput("pasteText", "length=" + text.length());
        inputCoordinator.runInput("pasteText", () -> doPasteText(text));
    }

    @Override
    public void typeTextUnicode(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        traceInput("typeTextUnicode", "length=" + text.length());
        inputCoordinator.runInput("typeTextUnicode", () -> doTypeTextUnicode(text));
    }

    @Override
    public void scrollDown(int clicks) {
        traceInput("scrollDown", "clicks=" + clicks);
        inputCoordinator.runInput("scrollDown", () -> doScroll(-120 * clicks));
    }

    @Override
    public void scrollUp(int clicks) {
        traceInput("scrollUp", "clicks=" + clicks);
        inputCoordinator.runInput("scrollUp", () -> doScroll(120 * clicks));
    }

    /**
     * 新手 §8.2 hold-sweep: presses the left button at the start point, sweeps left→right→left twice
     * per row while stepping down {@code rowStepPx}, and returns with the button STILL HELD so the
     * caller can read the progress counter before deciding to continue or release.
     */
    @Override
    public void holdSweepWithoutRelease(int startX, int startY, int leftX, int rightX,
                                        int endY, int rowStepPx) {
        traceInput("holdSweep", "start=(" + startX + "," + startY + ") x=[" + leftX + "," + rightX
                + "] endY=" + endY + " step=" + rowStepPx);
        inputCoordinator.runInput("holdSweep",
                () -> doHoldSweep(startX, startY, leftX, rightX, endY, rowStepPx));
    }

    /**
     * Continues a retained sweep while the same worker transaction still owns LEFT_DOWN.
     * No additional button-down or button-up event is emitted.
     */
    @Override
    public void sweepWhileLeftHeld(int startX, int startY, int leftX, int rightX,
                                   int endY, int rowStepPx) {
        traceInput("sweepWhileLeftHeld",
                "start=(" + startX + "," + startY + ") x=[" + leftX + "," + rightX
                        + "] endY=" + endY + " step=" + rowStepPx);
        inputCoordinator.runInput("sweepWhileLeftHeld",
                () -> {
                    moveCursorToLogicalPoint(startX, startY);
                    TaskSleep.sleep(120);
                    sweepRows(startX, startY, leftX, rightX, endY, rowStepPx);
                });
    }

    /** Releases a button held by {@link #holdSweepWithoutRelease}; safe to call when nothing is held. */
    @Override
    public void releaseLeftButton() {
        traceInput("releaseLeftButton", "");
        inputCoordinator.runInput("releaseLeftButton",
                () -> sendInput(buildMouseInput(FLAG_MOUSE_LEFT_UP)));
    }

    private void doHoldSweep(int startX, int startY, int leftX, int rightX, int endY, int rowStepPx) {
        moveCursorToLogicalPoint(startX, startY);
        TaskSleep.sleep(120);
        sendInput(buildMouseInput(FLAG_MOUSE_LEFT_DOWN));
        TaskSleep.sleep(150);
        sweepRows(startX, startY, leftX, rightX, endY, rowStepPx);
    }

    private void sweepRows(int startX, int startY, int leftX, int rightX,
                           int endY, int rowStepPx) {
        int step = Math.max(1, rowStepPx);
        for (int y = startY; y <= endY; y += step) {
            // Two full horizontal round trips per row, at ordinary speed (§8.2-2/§8.2-6).
            sweepRow(y, leftX, rightX);
            sweepRow(y, leftX, rightX);
        }
        // Deliberately no LEFT_UP: the caller reads progress while the button stays down (§8.2-1/4).
        log.info("Physical hold-sweep finished without release: start=({},{}) x=[{},{}] endY={} step={}",
                startX, startY, leftX, rightX, endY, step);
    }

    private void sweepRow(int y, int leftX, int rightX) {
        moveCursorToLogicalPoint(leftX, y);
        TaskSleep.sleep(15);
        moveCursorToLogicalPoint(rightX, y);
        TaskSleep.sleep(15);
        moveCursorToLogicalPoint(leftX, y);
        TaskSleep.sleep(15);
    }

    public void dragAndDrop(int startX, int startY, int endX, int endY) {
        traceInput("dragAndDrop", "from=(" + startX + "," + startY + ") to=(" + endX + "," + endY + ")");
        inputCoordinator.runInput("dragAndDrop", () -> doDragAndDrop(startX, startY, endX, endY));
    }

    private void traceInput(String operation, String detail) {
        WindowRuntimeContext context = windowTaskContextHolder.rawCurrent().orElse(null);
        WindowNativeBinding binding = context == null ? null : context.getNativeBinding();
        String source = inputCoordinator.currentInputActionName();
        if (source == null || source.isBlank()) {
            source = "direct:" + operation;
        }
        log.info("[INPUT_TRACE] physical operation={} source={} windowId={} hwnd={} role={} title={} {}",
                operation,
                source,
                context == null ? "-" : context.getWindowId(),
                binding == null ? "-" : binding.getNativeHandle(),
                context == null ? "-" : context.getRole(),
                binding == null ? "" : binding.getTitle(),
                detail == null ? "" : detail);
    }

    private void logClickForeground(String stage, int x, int y) {
        WindowRuntimeContext context = windowTaskContextHolder.rawCurrent().orElse(null);
        WindowNativeBinding binding = context == null ? null : context.getNativeBinding();
        Long targetHwnd = binding == null ? null : WindowHandleParser.parseHandle(binding.getNativeHandle());
        Long foregroundHwnd = currentForegroundHwnd();
        log.info("[INPUT_FOCUS_TRACE] operation=clickLeft stage={} source={} windowId={} targetHwnd={} foregroundHwnd={} sameAsTarget={} x={} y={} title={}",
                stage,
                inputCoordinator.currentInputActionName(),
                context == null ? "-" : context.getWindowId(),
                targetHwnd == null ? "-" : targetHwnd,
                foregroundHwnd == null ? "-" : foregroundHwnd,
                targetHwnd != null && targetHwnd.equals(foregroundHwnd),
                x,
                y,
                binding == null ? "" : binding.getTitle());
    }

    private Long currentForegroundHwnd() {
        var foreground = User32.INSTANCE.GetForegroundWindow();
        return foreground == null ? null : Pointer.nativeValue(foreground.getPointer());
    }

    private void doClick(int x, int y, int delayMs, int downFlag, int upFlag) {
        moveCursorToLogicalPoint(x, y);
        sendInput(buildMouseInput(downFlag));
        TaskSleep.sleep(delayMs);
        sendInput(buildMouseInput(upFlag));
    }

    private void doDragAndDrop(int startX, int startY, int endX, int endY) {
        moveCursorToLogicalPoint(startX, startY);
        TaskSleep.sleep(200);
        sendInput(buildMouseInput(FLAG_MOUSE_LEFT_DOWN));
        TaskSleep.sleep(300);

        int steps = 25;
        for (int i = 1; i <= steps; i++) {
            int currentX = startX + (endX - startX) * i / steps;
            int currentY = startY + (endY - startY) * i / steps;
            moveCursorToLogicalPoint(currentX, currentY);
            TaskSleep.sleep(15);
        }

        TaskSleep.sleep(200);
        sendInput(buildMouseInput(FLAG_MOUSE_LEFT_UP));
        TaskSleep.sleep(150);
        log.info("Physical drag completed: ({},{}) -> ({},{})", startX, startY, endX, endY);
    }

    private void doHoldCtrl() {
        try {
            /*
             * DHXY listens more reliably to hardware scan codes, the same way Alt+number
             * shortcuts are sent below. VK_CONTROL can be accepted by Windows but ignored by
             * the game client, which makes Ctrl-hover NPC menus never appear. Robot is sent as
             * an additional focused-input fallback because Ctrl is held across a following mouse
             * movement rather than delivered as a simple key stroke.
             */
            sendInput(buildKeyboardScanInput(SCAN_LCTRL, false));
            sendRobotCtrl(false);
        } catch (Exception e) {
            log.warn("[Input] hold Ctrl failed: {}", e.getMessage());
        }
    }

    private void doReleaseCtrl() {
        try {
            sendInput(buildKeyboardScanInput(SCAN_LCTRL, true));
            sendRobotCtrl(true);
        } catch (Exception e) {
            log.warn("[Input] release Ctrl failed: {}", e.getMessage());
        }
    }

    /**
     * Send a focused Robot Ctrl event as a compatibility layer for Ctrl-hover menus.
     *
     * @param keyUp true to release Ctrl, false to press and hold it.
     */
    private void sendRobotCtrl(boolean keyUp) {
        try {
            Robot robot = new Robot();
            if (keyUp) {
                robot.keyRelease(KeyEvent.VK_CONTROL);
            } else {
                robot.keyPress(KeyEvent.VK_CONTROL);
            }
            robot.delay(20);
        } catch (AWTException e) {
            log.debug("[Input] Robot Ctrl fallback unavailable: {}", e.getMessage());
        }
    }

    private void doPressEnter() {
        try {
            sendInput(buildKeyboardScanInput(SCAN_ENTER, false));
            TaskSleep.sleep(60);
            sendInput(buildKeyboardScanInput(SCAN_ENTER, true));
            TaskSleep.sleep(60);
        } catch (Exception e) {
            log.warn("[Input] Enter send failed: {}", e.getMessage());
        }
    }

    private void pressCtrlScan(int scanCode, String label) {
        try {
            sendInput(buildKeyboardScanInput(SCAN_LCTRL, false));
            TaskSleep.sleep(60);
            sendInput(buildKeyboardScanInput(scanCode, false));
            TaskSleep.sleep(80);
            sendInput(buildKeyboardScanInput(scanCode, true));
            TaskSleep.sleep(60);
            sendInput(buildKeyboardScanInput(SCAN_LCTRL, true));
            TaskSleep.sleep(160);
        } catch (Exception e) {
            log.warn("[Input] {} send failed: {}", label, e.getMessage());
            try {
                sendInput(buildKeyboardScanInput(scanCode, true));
                sendInput(buildKeyboardScanInput(SCAN_LCTRL, true));
            } catch (Exception ignored) {
            }
        }
    }

    private void doPasteText(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            sendInput(buildKeyboardInput(VK_CONTROL, false));
            TaskSleep.sleep(50);
            sendInput(buildKeyboardInput(VK_V, false));
            TaskSleep.sleep(50);
            sendInput(buildKeyboardInput(VK_V, true));
            TaskSleep.sleep(50);
            sendInput(buildKeyboardInput(VK_CONTROL, true));
            log.info("Pasted text: {}", text);
        } catch (Exception e) {
            doReleaseCtrl();
        }
    }

    private void doTypeTextUnicode(String text) {
        try {
            for (char c : text.toCharArray()) {
                sendInput(buildUnicodeInput(c, false));
                sendInput(buildUnicodeInput(c, true));
                TaskSleep.sleep(20);
            }
        } catch (Exception e) {
            log.warn("[Input] Unicode typing failed: {}", e.getMessage());
        }
    }

    private void doScroll(int wheelDelta) {
        try {
            INPUT input = new INPUT();
            input.type = new DWORD(WinUser.INPUT.INPUT_MOUSE);
            input.input.setType(UNION_FIELD_MOUSE);
            input.input.mi.dwFlags = new DWORD(FLAG_MOUSE_WHEEL);
            input.input.mi.mouseData = new DWORD(wheelDelta);
            sendInput(input);
            TaskSleep.sleep(50);
        } catch (Exception ignored) {
        }
    }

    private void pressAltScan(int scanCode, String label) {
        try {
            TaskSleep.sleep(200);
            sendInput(buildKeyboardScanInput(SCAN_LALT, false));
            TaskSleep.sleep(60);
            sendInput(buildKeyboardScanInput(scanCode, false));
            TaskSleep.sleep(80);
            sendInput(buildKeyboardScanInput(scanCode, true));
            TaskSleep.sleep(60);
            sendInput(buildKeyboardScanInput(SCAN_LALT, true));
        } catch (Exception e) {
            log.warn("[Input] {} send failed: {}", label, e.getMessage());
        } finally {
            try {
                sendInput(buildKeyboardScanInput(SCAN_LALT, true));
            } catch (Exception ignored) {
            }
        }
    }

    private static void sendInput(INPUT input) {
        User32.INSTANCE.SendInput(INPUT_ARRAY_SIZE_ONE, new INPUT[]{input}, input.size());
    }

    private void moveCursorToLogicalPoint(int x, int y) {
        double scale = coordinateHelper.getScaleRatio();
        int physicalX = (int) Math.round(x * scale);
        int physicalY = (int) Math.round(y * scale);
        POINT before = new POINT();
        POINT after = new POINT();
        for (int attempt = 1; attempt <= CURSOR_MOVE_MAX_ATTEMPTS; attempt++) {
            User32.INSTANCE.GetCursorPos(before);
            boolean moved = User32.INSTANCE.SetCursorPos(physicalX, physicalY);
            boolean readAfter = User32.INSTANCE.GetCursorPos(after);
            boolean reached = readAfter
                    && Math.abs(after.x - physicalX) <= 1
                    && Math.abs(after.y - physicalY) <= 1;
            log.info("[INPUT_CURSOR_TRACE] requestedLogical=({}, {}) requestedPhysical=({}, {}) attempt={}/{} before=({}, {}) after=({}, {}) setCursorPos={} readAfter={} reached={}",
                    x, y, physicalX, physicalY, attempt, CURSOR_MOVE_MAX_ATTEMPTS,
                    before.x, before.y, after.x, after.y, moved, readAfter, reached);
            if (reached) {
                return;
            }
            if (attempt < CURSOR_MOVE_MAX_ATTEMPTS
                    && !TaskSleep.sleep(CURSOR_MOVE_RETRY_DELAY_MS)) {
                break;
            }
        }
        throw new IllegalStateException("Physical cursor move failed after " + CURSOR_MOVE_MAX_ATTEMPTS
                + " attempts: target=(" + physicalX + "," + physicalY
                + ") actual=(" + after.x + "," + after.y + ")");
    }

    private static INPUT buildMouseInput(int mouseEventFlag) {
        INPUT input = new INPUT();
        input.type = new DWORD(WinUser.INPUT.INPUT_MOUSE);
        input.input.setType(UNION_FIELD_MOUSE);
        input.input.mi.dwFlags = new DWORD(mouseEventFlag);
        return input;
    }

    private static INPUT buildKeyboardInput(int virtualKey, boolean keyUp) {
        INPUT input = new INPUT();
        input.type = new DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType(UNION_FIELD_KEYBOARD);
        input.input.ki.wVk = new WORD(virtualKey);
        input.input.ki.dwFlags = new DWORD(keyUp ? FLAG_KEY_UP : 0);
        return input;
    }

    private static INPUT buildKeyboardScanInput(int scanCode, boolean keyUp) {
        INPUT input = new INPUT();
        input.type = new DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType(UNION_FIELD_KEYBOARD);
        input.input.ki.wVk = new WORD(0);
        input.input.ki.wScan = new WORD(scanCode);
        input.input.ki.dwFlags = new DWORD(FLAG_KEY_SCANCODE | (keyUp ? FLAG_KEY_UP : 0));
        return input;
    }

    private static INPUT buildUnicodeInput(char c, boolean keyUp) {
        INPUT input = new INPUT();
        input.type = new DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType(UNION_FIELD_KEYBOARD);
        input.input.ki.wVk = new WORD(0);
        input.input.ki.wScan = new WORD(c);
        input.input.ki.dwFlags = new DWORD(FLAG_KEY_UNICODE | (keyUp ? FLAG_KEY_UP : 0));
        return input;
    }

}
