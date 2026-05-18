package com.bot.dhxy.driver;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.input.GlobalInputLock;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.tools.CoordinateHelper;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.INPUT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

@Component
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
    private static final int SCAN_1 = 0x02;
    private static final int SCAN_2 = 0x03;
    private static final int SCAN_4 = 0x05;
    private static final int SCAN_8 = 0x09;
    private static final int SCAN_E = 0x12;
    private static final int SCAN_Q = 0x10;
    private static final int SCAN_ENTER = 0x1C;

    private static final String UNION_FIELD_MOUSE = "mi";
    private static final String UNION_FIELD_KEYBOARD = "ki";
    private static final DWORD INPUT_ARRAY_SIZE_ONE = new DWORD(1);

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final GlobalInputLock globalInputLock;

    @Override
    public void clickLeft(int x, int y, int delayMs) {
        globalInputLock.runWithLock(() -> doClick(x, y, delayMs, FLAG_MOUSE_LEFT_DOWN, FLAG_MOUSE_LEFT_UP));
    }

    @Override
    public void clickRight(int x, int y, int delayMs) {
        globalInputLock.runWithLock(() -> doClick(x, y, delayMs, FLAG_MOUSE_RIGHT_DOWN, FLAG_MOUSE_RIGHT_UP));
    }

    @Override
    public void doubleRightClick(int x, int y, int clickDelayMs, int intervalMs) {
        globalInputLock.runWithLock(() -> doClick(x, y, clickDelayMs, FLAG_MOUSE_RIGHT_DOWN, FLAG_MOUSE_RIGHT_UP));
        sleepQuietly(intervalMs);
        globalInputLock.runWithLock(() -> doClick(x, y, clickDelayMs, FLAG_MOUSE_RIGHT_DOWN, FLAG_MOUSE_RIGHT_UP));
    }

    @Override
    public void ctrlClickNpcTarget(int npcX, int npcY, int yellowNpcX, int yellowNpcY, int delayMs) {
    }

    @Override
    public void moveMouse(int x, int y) {
        globalInputLock.runWithLock(() -> moveCursorToLogicalPoint(x, y));
    }

    @Override
    public void holdCtrl() {
        globalInputLock.runWithLock(this::doHoldCtrl);
    }

    @Override
    public void releaseCtrl() {
        globalInputLock.runWithLock(this::doReleaseCtrl);
    }

    @Override
    public void pressAlt1() {
        globalInputLock.runWithLock(() -> pressAltScan(SCAN_1, "ALT+1"));
    }

    @Override
    public void pressAlt2() {
        globalInputLock.runWithLock(() -> pressAltScan(SCAN_2, "ALT+2"));
    }

    @Override
    public void pressAlt4() {
        globalInputLock.runWithLock(() -> pressAltScan(SCAN_4, "ALT+4"));
    }

    @Override
    public void pressAlt8() {
        globalInputLock.runWithLock(() -> pressAltScan(SCAN_8, "ALT+8"));
    }

    @Override
    public void pressAltE() {
        globalInputLock.runWithLock(() -> pressAltScan(SCAN_E, "ALT+E"));
    }

    @Override
    public void pressAltQ() {
        globalInputLock.runWithLock(() -> pressAltScan(SCAN_Q, "ALT+Q"));
    }

    @Override
    public void pressEnter() {
        globalInputLock.runWithLock(this::doPressEnter);
    }

    @Override
    public void pasteText(String text) {
        if (text == null) {
            return;
        }
        globalInputLock.runWithLock(() -> doPasteText(text));
    }

    @Override
    public void typeTextUnicode(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        globalInputLock.runWithLock(() -> doTypeTextUnicode(text));
    }

    @Override
    public void scrollDown(int clicks) {
        globalInputLock.runWithLock(() -> doScroll(-120 * clicks));
    }

    @Override
    public void scrollUp(int clicks) {
        globalInputLock.runWithLock(() -> doScroll(120 * clicks));
    }

    @Override
    public void dragAndDrop(int startX, int startY, int endX, int endY) {
        globalInputLock.runWithLock(() -> doDragAndDrop(startX, startY, endX, endY));
    }

    private void doClick(int x, int y, int delayMs, int downFlag, int upFlag) {
        moveCursorToLogicalPoint(x, y);
        sendInput(buildMouseInput(downFlag));
        sleepQuietly(delayMs);
        sendInput(buildMouseInput(upFlag));
    }

    private void doDragAndDrop(int startX, int startY, int endX, int endY) {
        moveCursorToLogicalPoint(startX, startY);
        sleepQuietly(200);
        sendInput(buildMouseInput(FLAG_MOUSE_LEFT_DOWN));
        sleepQuietly(300);

        int steps = 25;
        for (int i = 1; i <= steps; i++) {
            int currentX = startX + (endX - startX) * i / steps;
            int currentY = startY + (endY - startY) * i / steps;
            moveCursorToLogicalPoint(currentX, currentY);
            sleepQuietly(15);
        }

        sleepQuietly(200);
        sendInput(buildMouseInput(FLAG_MOUSE_LEFT_UP));
        sleepQuietly(150);
        log.info("Physical drag completed: ({},{}) -> ({},{})", startX, startY, endX, endY);
    }

    private void doHoldCtrl() {
        try {
            if (!tracker.bringWindowToFront()) {
                return;
            }
            sendInput(buildKeyboardInput(VK_CONTROL, false));
        } catch (Exception e) {
            log.warn("[Input] hold Ctrl failed: {}", e.getMessage());
        }
    }

    private void doReleaseCtrl() {
        try {
            sendInput(buildKeyboardInput(VK_CONTROL, true));
        } catch (Exception e) {
            log.warn("[Input] release Ctrl failed: {}", e.getMessage());
        }
    }

    private void doPressEnter() {
        try {
            sendInput(buildKeyboardScanInput(SCAN_ENTER, false));
            sleepQuietly(60);
            sendInput(buildKeyboardScanInput(SCAN_ENTER, true));
            sleepQuietly(60);
        } catch (Exception e) {
            log.warn("[Input] Enter send failed: {}", e.getMessage());
        }
    }

    private void doPasteText(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            sendInput(buildKeyboardInput(VK_CONTROL, false));
            sleepQuietly(50);
            sendInput(buildKeyboardInput(VK_V, false));
            sleepQuietly(50);
            sendInput(buildKeyboardInput(VK_V, true));
            sleepQuietly(50);
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
                sleepQuietly(20);
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
            sleepQuietly(50);
        } catch (Exception ignored) {
        }
    }

    private void pressAltScan(int scanCode, String label) {
        try {
            if (!tracker.bringWindowToFront()) {
                return;
            }
            sleepQuietly(200);
            sendInput(buildKeyboardScanInput(SCAN_LALT, false));
            sleepQuietly(60);
            sendInput(buildKeyboardScanInput(scanCode, false));
            sleepQuietly(80);
            sendInput(buildKeyboardScanInput(scanCode, true));
            sleepQuietly(60);
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
        User32.INSTANCE.SetCursorPos(physicalX, physicalY);
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

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
