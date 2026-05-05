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

    private static final int VK_CONTROL = 0x11;
    private static final int VK_V = 0x56;

    private static final int SCAN_LALT = 0x38;
    private static final int SCAN_1 = 0x02;
    private static final int SCAN_2 = 0x03;
    private static final int SCAN_4 = 0x05; // 🌟 新增：数字键 4 的硬件扫描码
    private static final int SCAN_ENTER = 0x1C;

    private static final String UNION_FIELD_MOUSE = "mi";
    private static final String UNION_FIELD_KEYBOARD = "ki";

    private static final DWORD INPUT_ARRAY_SIZE_ONE = new DWORD(1);

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;

    private static final int FLAG_KEY_UNICODE = 0x0004;

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
        // 废弃不用
    }

    // ==========================================
    // 🌟 底层开关能力实现
    // ==========================================

    @Override
    public void moveMouse(int x, int y) {
        moveCursorToLogicalPoint(x, y);
    }

    @Override
    public void holdCtrl() {
        try {
            if (!tracker.bringWindowToFront()) return;
            // 发送按下信号
            sendInput(buildKeyboardInput(VK_CONTROL, false));
        } catch (Exception e) {
            System.err.println("❌ [Input] 按住 Ctrl 失败: " + e.getMessage());
        }
    }

    @Override
    public void releaseCtrl() {
        try {
            // 发送弹起信号
            sendInput(buildKeyboardInput(VK_CONTROL, true));
        } catch (Exception e) {
            System.err.println("❌ [Input] 释放 Ctrl 失败: " + e.getMessage());
        }
    }

    // ==========================================

    @Override
    public void pressAlt2() {
        try {
            if (!tracker.bringWindowToFront()) return;
            Thread.sleep(200);
            sendInput(buildKeyboardScanInput(SCAN_LALT, false));
            Thread.sleep(60);
            sendInput(buildKeyboardScanInput(SCAN_2, false));
            Thread.sleep(80);
            sendInput(buildKeyboardScanInput(SCAN_2, true));
            Thread.sleep(60);
            sendInput(buildKeyboardScanInput(SCAN_LALT, true));
        } catch (Exception e) {
            System.err.println("❌ [Input] 按键发送失败: " + e.getMessage());
        }
    }

    // 🌟 新增：完美复刻原有的组合键逻辑，实现 ALT + 4
    @Override
    public void pressAlt4() {
        try {
            if (!tracker.bringWindowToFront()) return;
            Thread.sleep(200); // 前置停顿，等待游戏响应
            sendInput(buildKeyboardScanInput(SCAN_LALT, false)); // 按下 ALT
            Thread.sleep(60);
            sendInput(buildKeyboardScanInput(SCAN_4, false));    // 按下 4
            Thread.sleep(80);
            sendInput(buildKeyboardScanInput(SCAN_4, true));     // 松开 4
            Thread.sleep(60);
            sendInput(buildKeyboardScanInput(SCAN_LALT, true));  // 松开 ALT
        } catch (Exception e) {
            System.err.println("❌ [Input] ALT+4 按键发送失败: " + e.getMessage());
        } finally {
            // 🛡️ 兜底保命：防止报错导致 ALT 被锁死
            try {
                sendInput(buildKeyboardScanInput(SCAN_LALT, true));
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void pressEnter() {
        try {
            sendInput(buildKeyboardScanInput(SCAN_ENTER, false));
            Thread.sleep(60);
            sendInput(buildKeyboardScanInput(SCAN_ENTER, true));
            Thread.sleep(60);
        } catch (Exception e) {
            System.err.println("❌ [Input] 回车键发送失败: " + e.getMessage());
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
            log.info("粘贴文本**{}**成功", text);
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
            System.err.println("❌ [Input] Unicode 打字失败: " + e.getMessage());
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
        } catch (Exception e) {}
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
        } catch (Exception e) {}
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

    @Override
    public void pressAlt1() {
        try {
            if (!tracker.bringWindowToFront()) return;
            Thread.sleep(200); // 前置停顿，等待游戏响应
            sendInput(buildKeyboardScanInput(SCAN_LALT, false)); // 按下 ALT
            Thread.sleep(60);
            sendInput(buildKeyboardScanInput(SCAN_1, false));    // 按下 1
            Thread.sleep(80);
            sendInput(buildKeyboardScanInput(SCAN_1, true));     // 松开 1
            Thread.sleep(60);
            sendInput(buildKeyboardScanInput(SCAN_LALT, true));  // 松开 ALT
            log.debug("⌨️ [物理驱动] 执行 Alt+1 呼叫小地图");
        } catch (Exception e) {
            System.err.println("❌ [Input] ALT+1 按键发送失败: " + e.getMessage());
        } finally {
            // 🛡️ 兜底保命：防止报错导致 ALT 被锁死
            try {
                sendInput(buildKeyboardScanInput(SCAN_LALT, true));
            } catch (Exception ignored) {}
        }
    }
}