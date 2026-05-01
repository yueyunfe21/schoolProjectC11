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

    // 🌟 纯正的硬件扫描码 (DirectInput 唯一认这个)
    private static final int SCAN_LALT = 0x38;
    private static final int SCAN_2 = 0x03; // 主键盘数字 2 的硬件码

    private static final int SCAN_ENTER = 0x1C; // 🌟 新增：主键盘的 Enter 键物理扫描码

    private static final String UNION_FIELD_MOUSE = "mi";
    private static final String UNION_FIELD_KEYBOARD = "ki";



    private static final DWORD INPUT_ARRAY_SIZE_ONE = new DWORD(1);
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper; // 🌟 注入

    private static final int FLAG_KEY_UNICODE = 0x0004; // 🌟 告诉系统：我发的是 Unicode 字符，不是键盘按键！

    @Override
    public void clickLeft(int x, int y, int delayMs) {
        try {
            // ==========================================
            // 🌟 核心换算：Java 给的是逻辑坐标，Windows 底层要的是物理坐标，必须乘回去！
            // ==========================================
            double scale = coordinateHelper.getScaleRatio();
            int physicalX = (int) Math.round(x * scale);
            int physicalY = (int) Math.round(y * scale);

            // 告诉 Windows，去点真实的物理屏幕像素！
            User32.INSTANCE.SetCursorPos(physicalX, physicalY);

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
            double scale = coordinateHelper.getScaleRatio();
            int physicalX = (int) Math.round(x * scale);
            int physicalY = (int) Math.round(y * scale);

            User32.INSTANCE.SetCursorPos(physicalX, physicalY);

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
    public void pressAlt2() {
        try {
            System.out.println("⌨️ [Input] 开始发送底层组合键 Alt+2...");

            // 1. 确保游戏在最前面，但【坚决不点击屏幕中心】以免角色乱跑
            if (!tracker.bringWindowToFront()) {
                System.out.println("❌ [Input] 游戏未置顶，放弃按键");
                return;
            }

            // 稍作停顿，等窗口彻底切过来
            Thread.sleep(200);

            // 2. 🌟 唯一指定通道：硬件扫描码 (模拟真实物理键盘)
            // 按下 左Alt
            sendInput(buildKeyboardScanInput(SCAN_LALT, false));
            Thread.sleep(60); // 必须停顿！给游戏引擎反应时间

            // 按下 2
            sendInput(buildKeyboardScanInput(SCAN_2, false));
            Thread.sleep(80); // 键盘按到底的物理停顿

            // 松开 2
            sendInput(buildKeyboardScanInput(SCAN_2, true));
            Thread.sleep(60);

            // 松开 左Alt
            sendInput(buildKeyboardScanInput(SCAN_LALT, true));

            System.out.println("✅ [Input] Alt+2 发送完毕");

        } catch (Exception e) {
            System.err.println("❌ [Input] 按键发送失败: " + e.getMessage());
        }
    }

    @Override
    public void pressEnter() {
        try {

            System.out.println("⌨️ [Input] 准备敲击回车键...");

            // 🌟 依然使用最底层的硬件扫描码通道
            sendInput(buildKeyboardScanInput(SCAN_ENTER, false)); // 按下回车
            Thread.sleep(60); // 必须停顿，模拟真实人类按压键盘的时间

            sendInput(buildKeyboardScanInput(SCAN_ENTER, true));  // 松开回车
            Thread.sleep(60); // 敲击完成后的余留时间

            System.out.println("✅ [Input] 回车键发送完毕");
        } catch (Exception e) {
            System.err.println("❌ [Input] 回车键发送失败: " + e.getMessage());
        }
    }

    @Override
    public void pasteText(String text) {
        if (text == null) return;
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);

            // 粘贴直接用虚拟键即可，因为这通常是给操作系统的输入框发信号
            sendInput(buildKeyboardInput(VK_CONTROL, false));
            Thread.sleep(50);
            sendInput(buildKeyboardInput(VK_V, false));
            Thread.sleep(50);
            sendInput(buildKeyboardInput(VK_V, true));
            Thread.sleep(50);
            sendInput(buildKeyboardInput(VK_CONTROL, true));
            log.info("粘贴文本**{}**成功", text);
        } catch (Exception e) {
            System.err.println("❌ [Input] 粘贴文本失败: " + e.getMessage());
        }
    }

    private static void sendInput(INPUT input) {
        User32.INSTANCE.SendInput(INPUT_ARRAY_SIZE_ONE, new INPUT[]{input}, input.size());
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
        input.input.ki.wVk = new WORD(0); // 🌟 用扫描码时，虚拟键必须设为0
        input.input.ki.wScan = new WORD(scanCode);
        input.input.ki.dwFlags = new DWORD(FLAG_KEY_SCANCODE | (keyUp ? FLAG_KEY_UP : 0));
        return input;
    }

    private static INPUT buildUnicodeInput(char c, boolean keyUp) {
        INPUT input = new INPUT();
        input.type = new DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType(UNION_FIELD_KEYBOARD);
        input.input.ki.wVk = new WORD(0); // 必须为0
        input.input.ki.wScan = new WORD(c); // 直接塞入汉字的 Unicode 码
        input.input.ki.dwFlags = new DWORD(FLAG_KEY_UNICODE | (keyUp ? FLAG_KEY_UP : 0));
        return input;
    }

    /**
     * 🌟 终极输入法：绕过剪贴板，直接将文字“打”进游戏
     */
    public void typeTextUnicode(String text) {
        if (text == null || text.isEmpty()) return;
        try {
            System.out.println("⌨️ [Input] 开始 Unicode 强制打字: " + text);
            for (char c : text.toCharArray()) {
                // 按下这个字
                sendInput(buildUnicodeInput(c, false));
                // 松开这个字
                sendInput(buildUnicodeInput(c, true));
                // 模拟人类打字间隔
                Thread.sleep(20);
            }
        } catch (Exception e) {
            System.err.println("❌ [Input] Unicode 打字失败: " + e.getMessage());
        }
    }

    @Override
    public void scrollDown(int clicks) {
        try {
            // Windows 底层规定：向下滚动是负数，一格是 120 (WHEEL_DELTA)
            int wheelMovement = -120 * clicks;

            INPUT input = new INPUT();
            input.type = new DWORD(WinUser.INPUT.INPUT_MOUSE);
            input.input.setType(UNION_FIELD_MOUSE);
            // 告诉 Windows 这是一次滚轮操作
            input.input.mi.dwFlags = new DWORD(FLAG_MOUSE_WHEEL);
            // 塞入滚动的数值 (-120, -240 等)
            input.input.mi.mouseData = new DWORD(wheelMovement);

            sendInput(input);
            System.out.println("🖱️ [Input] 鼠标向下滚动了 " + clicks + " 格");

            // 模拟人类滚动后的短暂停顿
            Thread.sleep(50);
        } catch (Exception e) {
            System.err.println("❌ [Input] 鼠标向下滚动失败: " + e.getMessage());
        }
    }

    @Override
    public void scrollUp(int clicks) {
        try {
            // 向上滚动是正数
            int wheelMovement = 120 * clicks;

            INPUT input = new INPUT();
            input.type = new DWORD(WinUser.INPUT.INPUT_MOUSE);
            input.input.setType(UNION_FIELD_MOUSE);
            input.input.mi.dwFlags = new DWORD(FLAG_MOUSE_WHEEL);
            input.input.mi.mouseData = new DWORD(wheelMovement);

            sendInput(input);
            System.out.println("🖱️ [Input] 鼠标向上滚动了 " + clicks + " 格");

            Thread.sleep(50);
        } catch (Exception e) {
            System.err.println("❌ [Input] 鼠标向上滚动失败: " + e.getMessage());
        }
    }


}
