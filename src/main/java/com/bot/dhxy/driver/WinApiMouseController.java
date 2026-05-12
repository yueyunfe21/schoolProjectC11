package com.bot.dhxy.driver;

import com.bot.dhxy.config.InputProvider;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.tools.CoordinateHelper;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.INPUT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinDef.LPARAM;

import java.awt.*;
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
    private static final int SCAN_E = 0x12;
    private static final int SCAN_Q = 0x10;
    private static final int SCAN_ENTER = 0x1C;
    // 把原来的 0x0A 改成 0x09 ！！！
    private static final int SCAN_8 = 0x09; // 键盘顶排数字 8 的正确硬件扫描码

    private static final String UNION_FIELD_MOUSE = "mi";
    private static final String UNION_FIELD_KEYBOARD = "ki";
    private static final DWORD INPUT_ARRAY_SIZE_ONE = new DWORD(1);

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;



    @Override
    public void pressAlt8() {
        pressAltScan(SCAN_8, "ALT+8");
    }

    @Override
    public void dragAndDrop(int startX, int startY, int endX, int endY) {
        try {
            // 1. 移动到起点并稳住
            moveCursorToLogicalPoint(startX, startY);
            Thread.sleep(200);

            // 2. 狠狠按下左键（不松开）
            INPUT inputDown = buildMouseInput(FLAG_MOUSE_LEFT_DOWN);
            sendInput(inputDown);
            Thread.sleep(300); // 给游戏引擎一点时间反应“已被按住”

            // 🌟 3. 核心修复：平滑滑轨算法 (模拟真人拖拽)
            int steps = 25; // 把整段距离切成 25 步
            for (int i = 1; i <= steps; i++) {
                // 利用线性插值算出现在这一步的落脚点
                int currentX = startX + (endX - startX) * i / steps;
                int currentY = startY + (endY - startY) * i / steps;

                moveCursorToLogicalPoint(currentX, currentY);
                Thread.sleep(15); // 极其短暂的停顿，连起来就是极其丝滑的滑动！
            }

            // 4. 到达终点，屏住呼吸稳一下，防止惯性漂移
            Thread.sleep(200);

            // 5. 潇洒松开左键
            INPUT inputUp = buildMouseInput(FLAG_MOUSE_LEFT_UP);
            sendInput(inputUp);
            Thread.sleep(150);

            log.info("🖱️ 物理滑轨拖拽完成：({},{}) -> ({},{})", startX, startY, endX, endY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void clickLeft(int x, int y, int delayMs) {
        try {
            moveCursorToLogicalPoint(x, y);
            INPUT inputDown = buildMouseInput(FLAG_MOUSE_LEFT_DOWN);
            User32.INSTANCE.SendInput(INPUT_ARRAY_SIZE_ONE, new INPUT[]{inputDown}, inputDown.size());
            Thread.sleep(delayMs);
            INPUT inputUp = buildMouseInput(FLAG_MOUSE_LEFT_UP);
            User32.INSTANCE.SendInput(INPUT_ARRAY_SIZE_ONE, new INPUT[]{inputUp}, inputUp.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    @Override
    public void clickRight(int x, int y, int delayMs) {
        try {
            moveCursorToLogicalPoint(x, y);
            INPUT inputDown = buildMouseInput(FLAG_MOUSE_RIGHT_DOWN);
            User32.INSTANCE.SendInput(INPUT_ARRAY_SIZE_ONE, new INPUT[]{inputDown}, inputDown.size());
            Thread.sleep(delayMs);
            INPUT inputUp = buildMouseInput(FLAG_MOUSE_RIGHT_UP);
            User32.INSTANCE.SendInput(INPUT_ARRAY_SIZE_ONE, new INPUT[]{inputUp}, inputUp.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void doubleRightClick(int x, int y, int clickDelayMs, int intervalMs) {
        clickRight(x, y, clickDelayMs);
        try {
            Thread.sleep(intervalMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        clickRight(x, y, clickDelayMs);
    }

    @Override
    public void ctrlClickNpcTarget(int npcX, int npcY, int yellowNpcX, int yellowNpcY, int delayMs) {
    }

    @Override
    public void moveMouse(int x, int y) {
        moveCursorToLogicalPoint(x, y);
    }

    @Override
    public void holdCtrl() {
        try {
            if (!tracker.bringWindowToFront()) return;
            sendInput(buildKeyboardInput(VK_CONTROL, false));
        } catch (Exception e) {
            System.err.println("[Input] hold Ctrl failed: " + e.getMessage());
        }
    }

    @Override
    public void releaseCtrl() {
        try {
            sendInput(buildKeyboardInput(VK_CONTROL, true));
        } catch (Exception e) {
            System.err.println("[Input] release Ctrl failed: " + e.getMessage());
        }
    }

    @Override
    public void pressAlt1() {
        pressAltScan(SCAN_1, "ALT+1");
    }

    @Override
    public void pressAlt2() {
        pressAltScan(SCAN_2, "ALT+2");
    }

    @Override
    public void pressAlt4() {
        pressAltScan(SCAN_4, "ALT+4");
    }

    @Override
    public void pressAltE() {
        pressAltScan(SCAN_E, "ALT+E");
    }

    @Override
    public void pressAltQ() {
        pressAltScan(SCAN_Q, "ALT+Q");
    }

    @Override
    public void pressEnter() {
        try {
            sendInput(buildKeyboardScanInput(SCAN_ENTER, false));
            Thread.sleep(60);
            sendInput(buildKeyboardScanInput(SCAN_ENTER, true));
            Thread.sleep(60);
        } catch (Exception e) {
            System.err.println("[Input] Enter send failed: " + e.getMessage());
        }
    }

    @Override
    public void pasteText(String text) {
        if (text == null) return;
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            sendInput(buildKeyboardInput(VK_CONTROL, false));
            Thread.sleep(50);
            sendInput(buildKeyboardInput(VK_V, false));
            Thread.sleep(50);
            sendInput(buildKeyboardInput(VK_V, true));
            Thread.sleep(50);
            sendInput(buildKeyboardInput(VK_CONTROL, true));
            log.info("Pasted text: {}", text);
        } catch (Exception e) {
            releaseCtrl();
        }
    }

    @Override
    public void typeTextUnicode(String text) {
        if (text == null || text.isEmpty()) return;
        try {
            for (char c : text.toCharArray()) {
                sendInput(buildUnicodeInput(c, false));
                sendInput(buildUnicodeInput(c, true));
                Thread.sleep(20);
            }
        } catch (Exception e) {
            System.err.println("[Input] Unicode typing failed: " + e.getMessage());
        }
    }

    @Override
    public void scrollDown(int clicks) {
        try {
            INPUT input = new INPUT();
            input.type = new DWORD(WinUser.INPUT.INPUT_MOUSE);
            input.input.setType(UNION_FIELD_MOUSE);
            input.input.mi.dwFlags = new DWORD(FLAG_MOUSE_WHEEL);
            input.input.mi.mouseData = new DWORD(-120 * clicks);
            sendInput(input);
            Thread.sleep(50);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void scrollUp(int clicks) {
        try {
            INPUT input = new INPUT();
            input.type = new DWORD(WinUser.INPUT.INPUT_MOUSE);
            input.input.setType(UNION_FIELD_MOUSE);
            input.input.mi.dwFlags = new DWORD(FLAG_MOUSE_WHEEL);
            input.input.mi.mouseData = new DWORD(120 * clicks);
            sendInput(input);
            Thread.sleep(50);
        } catch (Exception ignored) {
        }
    }

    private void pressAltScan(int scanCode, String label) {
        try {
            if (!tracker.bringWindowToFront()) return;
            Thread.sleep(200);
            sendInput(buildKeyboardScanInput(SCAN_LALT, false));
            Thread.sleep(60);
            sendInput(buildKeyboardScanInput(scanCode, false));
            Thread.sleep(80);
            sendInput(buildKeyboardScanInput(scanCode, true));
            Thread.sleep(60);
            sendInput(buildKeyboardScanInput(SCAN_LALT, true));
        } catch (Exception e) {
            System.err.println("[Input] " + label + " send failed: " + e.getMessage());
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
}
