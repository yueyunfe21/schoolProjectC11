package com.bot.dhxy.core;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.config.VisionProvider;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GameClientTracker {

    private final VisionProvider eyes;
    private final BotProperties config;
    private final CoordinateHelper coordinateHelper;
    private final WindowTaskContextHolder windowTaskContextHolder;

    @Lazy
    @Autowired
    private InputProvider inputProvider;

    public static final String LATEST_VISION_PATH = "images/temp/latest_vision.png";
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;

    @Getter
    private int windowBaseX = -1;
    @Getter
    private int windowBaseY = -1;
    @Getter
    private String fullWindowTitle = "";

    @Getter
    private HWND gameHwnd = null;

    public boolean updateGlobalVision() {
        if (!checkBaseAddress()) return false;
        if (!bringWindowToFront()) {
            System.out.println("❌ 无法唤醒游戏，停止任务。");
            return false;
        }
        int x1 = windowBaseX;
        int y1 = windowBaseY;
        int x2 = x1 + WINDOW_WIDTH;
        int y2 = y1 + WINDOW_HEIGHT;
        return captureToFile("全局视野", LATEST_VISION_PATH, x1, y1, x2, y2);
    }

    public boolean locateWindow() {
        if (useBoundWindowIfAvailable()) {
            return true;
        }

        String target = config.getWindowKeyword();
        final HWND[] targetHwnd = {null};

        User32.INSTANCE.EnumWindows((hwnd, pointer) -> {
            if (!User32.INSTANCE.IsWindowVisible(hwnd)) {
                return true;
            }
            char[] windowText = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
            String wText = Native.toString(windowText).trim();
            if (wText.contains(target)) {
                targetHwnd[0] = hwnd;
                fullWindowTitle = wText;
                gameHwnd = hwnd;
                return false;
            }
            return true;
        }, null);

        if (targetHwnd[0] == null) {
            System.out.println("❌ [定位失败] 未找到包含关键字 [" + target + "] 的窗口");
            return false;
        }

        updateBaseFromHwnd(targetHwnd[0], fullWindowTitle);
        System.out.println("✅ [定位成功] 目标: " + fullWindowTitle + " | 窗口基址 X:" + windowBaseX + " Y:" + windowBaseY);
        return true;
    }

    public boolean captureToFile(String elementName, String savePath, int x1, int y1, int x2, int y2) {
        if (!checkBaseAddress()) return false;
        System.out.println("📸 [" + elementName + "] 正在截取画面保存至: " + savePath);
        return eyes.captureRegionToFile(savePath, x1, y1, x2, y2);
    }

    public boolean captureToFileWithShield(String elementName, String savePath, int x1, int y1, int x2, int y2) {
        if (!checkBaseAddress()) return false;
        System.out.println("🛡️ [装甲截图] 准备截取 " + elementName + ": 启动强制清屏 (ALT+4)...");
        inputProvider.pressAlt4();
        sleepQuietly(400);
        try {
            return eyes.captureRegionToFile(savePath, x1, y1, x2, y2);
        } finally {
            inputProvider.pressAlt4();
            System.out.println("🔰 [装甲截图] " + elementName + " 截图完毕，画面已恢复。");
        }
    }

    public BufferedImage captureToMemory(String elementName, int x1, int y1, int x2, int y2) {
        if (!checkBaseAddress()) return null;
        return eyes.captureRegionByCoordinates(x1, y1, x2, y2);
    }

    private boolean checkBaseAddress() {
        if (useBoundWindowIfAvailable()) {
            return true;
        }
        if (windowBaseX == -1 || gameHwnd == null) {
            return locateWindow();
        }
        updateBaseFromHwnd(gameHwnd, fullWindowTitle);
        return true;
    }

    private boolean useBoundWindowIfAvailable() {
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.current();
        if (current.isEmpty()) {
            return false;
        }
        WindowNativeBinding binding = current.get().getNativeBinding();
        if (binding == null || !binding.hasNativeHandle()) {
            return false;
        }
        HWND hwnd = toHwnd(binding.getNativeHandle());
        if (hwnd == null) {
            return false;
        }
        String title = binding.getTitle() == null || binding.getTitle().isBlank()
                ? current.get().getWindowId()
                : binding.getTitle();
        updateBaseFromHwnd(hwnd, title);
        return true;
    }

    private void updateBaseFromHwnd(HWND hwnd, String title) {
        RECT rect = new RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);
        double scale = coordinateHelper.getScaleRatio();
        windowBaseX = (int) (rect.left / scale);
        windowBaseY = (int) (rect.top / scale);
        gameHwnd = hwnd;
        fullWindowTitle = title == null ? "" : title;
    }

    public boolean bringWindowToFront() {
        if (!checkBaseAddress()) {
            return false;
        }
        System.out.println("🔄 正在将游戏窗口唤醒并置顶...");
        User32.INSTANCE.ShowWindow(gameHwnd, 9);
        User32.INSTANCE.SetForegroundWindow(gameHwnd);
        sleepQuietly(500);
        return true;
    }

    private HWND toHwnd(String handleText) {
        Long value = parseHandleValue(handleText);
        if (value == null || value <= 0) {
            return null;
        }
        return new HWND(new Pointer(value));
    }

    private Long parseHandleValue(String handleText) {
        if (handleText == null || handleText.isBlank()) {
            return null;
        }
        String value = handleText.trim();
        try {
            if (value.startsWith("0x") || value.startsWith("0X")) {
                return Long.parseUnsignedLong(value.substring(2), 16);
            }
            if (value.matches(".*[a-fA-F].*")) {
                return Long.parseUnsignedLong(value, 16);
            }
            try {
                return Long.parseUnsignedLong(value);
            } catch (NumberFormatException ignored) {
                return Long.parseUnsignedLong(value, 16);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void testBackgroundAlt8() {
        if (this.gameHwnd == null) {
            System.out.println("❌ gameHwnd 为空，请先执行 locateWindow() !");
            return;
        }
        System.out.println("🎯 直接使用缓存的句柄投递后台 Alt+8...");
        int WM_SYSKEYDOWN = 0x0104;
        int WM_SYSKEYUP = 0x0105;
        int VK_8 = 0x38;
        long lParamDown = (1 << 29) | (0x09 << 16) | 1;
        long lParamUp = (1L << 31) | (1 << 30) | (1 << 29) | (0x09 << 16) | 1;
        User32.INSTANCE.PostMessage(this.gameHwnd, WM_SYSKEYDOWN, new WPARAM(VK_8), new LPARAM(lParamDown));
        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        User32.INSTANCE.PostMessage(this.gameHwnd, WM_SYSKEYUP, new WPARAM(VK_8), new LPARAM(lParamUp));
        System.out.println("📩 后台指令投递完毕！");
    }
}
