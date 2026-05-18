package com.bot.dhxy.core;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.config.VisionProvider;
import com.bot.dhxy.input.GlobalInputLock;
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
    private final GlobalInputLock globalInputLock;

    @Lazy
    @Autowired
    private InputProvider inputProvider;

    public static final String LATEST_VISION_PATH = "images/temp/latest_vision.png";
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;

    /**
     * 单窗口旧流程使用 sharedState。
     * 多窗口任务线程使用 ThreadLocal state，避免不同窗口互相覆盖 gameHwnd/windowBaseX/windowBaseY。
     */
    private final TrackerState sharedState = new TrackerState();
    private final ThreadLocal<TrackerState> threadState = ThreadLocal.withInitial(TrackerState::new);

    public int getWindowBaseX() { return state().windowBaseX; }

    public int getWindowBaseY() { return state().windowBaseY; }

    public String getFullWindowTitle() { return state().fullWindowTitle; }

    public HWND getGameHwnd() { return state().gameHwnd; }

    public boolean updateGlobalVision() {
        return globalInputLock.callWithLock(() -> {
            if (!checkBaseAddress()) return false;
            if (!bringWindowToFrontWithoutLock()) {
                System.out.println("❌ 无法唤醒游戏，停止任务。");
                return false;
            }
            TrackerState s = state();
            int x1 = s.windowBaseX;
            int y1 = s.windowBaseY;
            int x2 = x1 + WINDOW_WIDTH;
            int y2 = y1 + WINDOW_HEIGHT;
            return captureToFileWithoutLock("全局视野", LATEST_VISION_PATH, x1, y1, x2, y2);
        });
    }

    public boolean locateWindow() {
        if (useBoundWindowIfAvailable()) {
            return true;
        }

        String target = config.getWindowKeyword();
        final HWND[] targetHwnd = {null};
        final String[] targetTitle = {""};

        User32.INSTANCE.EnumWindows((hwnd, pointer) -> {
            if (!User32.INSTANCE.IsWindowVisible(hwnd)) {
                return true;
            }
            char[] windowText = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
            String wText = Native.toString(windowText).trim();
            if (wText.contains(target)) {
                targetHwnd[0] = hwnd;
                targetTitle[0] = wText;
                return false;
            }
            return true;
        }, null);

        if (targetHwnd[0] == null) {
            System.out.println("❌ [定位失败] 未找到包含关键字 [" + target + "] 的窗口");
            return false;
        }

        updateBaseFromHwnd(targetHwnd[0], targetTitle[0]);
        TrackerState s = state();
        System.out.println("✅ [定位成功] 目标: " + s.fullWindowTitle + " | 窗口基址 X:" + s.windowBaseX + " Y:" + s.windowBaseY);
        return true;
    }

    public boolean captureToFile(String elementName, String savePath, int x1, int y1, int x2, int y2) {
        return globalInputLock.callWithLock(() -> captureToFileWithoutLock(elementName, savePath, x1, y1, x2, y2));
    }

    public boolean captureToFileWithShield(String elementName, String savePath, int x1, int y1, int x2, int y2) {
        return globalInputLock.callWithLock(() -> {
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
        });
    }

    public BufferedImage captureToMemory(String elementName, int x1, int y1, int x2, int y2) {
        return globalInputLock.callWithLock(() -> {
            if (!checkBaseAddress()) return null;
            return eyes.captureRegionByCoordinates(x1, y1, x2, y2);
        });
    }

    private boolean captureToFileWithoutLock(String elementName, String savePath, int x1, int y1, int x2, int y2) {
        if (!checkBaseAddress()) return false;
        System.out.println("📸 [" + elementName + "] 正在截取画面保存至: " + savePath);
        return eyes.captureRegionToFile(savePath, x1, y1, x2, y2);
    }

    private boolean checkBaseAddress() {
        if (useBoundWindowIfAvailable()) {
            return true;
        }
        TrackerState s = state();
        if (s.windowBaseX == -1 || s.gameHwnd == null) {
            return locateWindow();
        }
        updateBaseFromHwnd(s.gameHwnd, s.fullWindowTitle);
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
        TrackerState s = state();
        s.windowBaseX = (int) (rect.left / scale);
        s.windowBaseY = (int) (rect.top / scale);
        s.gameHwnd = hwnd;
        s.fullWindowTitle = title == null ? "" : title;
    }

    public boolean bringWindowToFront() {
        return globalInputLock.callWithLock(this::bringWindowToFrontWithoutLock);
    }

    private boolean bringWindowToFrontWithoutLock() {
        if (!checkBaseAddress()) {
            return false;
        }
        TrackerState s = state();
        System.out.println("🔄 正在将游戏窗口唤醒并置顶...");
        User32.INSTANCE.ShowWindow(s.gameHwnd, 9);
        User32.INSTANCE.SetForegroundWindow(s.gameHwnd);
        sleepQuietly(500);
        return true;
    }

    private TrackerState state() {
        return windowTaskContextHolder.current().isPresent() ? threadState.get() : sharedState;
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
        TrackerState s = state();
        if (s.gameHwnd == null) {
            System.out.println("❌ gameHwnd 为空，请先执行 locateWindow() !");
            return;
        }
        System.out.println("🎯 直接使用缓存的句柄投递后台 Alt+8...");
        int WM_SYSKEYDOWN = 0x0104;
        int WM_SYSKEYUP = 0x0105;
        int VK_8 = 0x38;
        long lParamDown = (1 << 29) | (0x09 << 16) | 1;
        long lParamUp = (1L << 31) | (1 << 30) | (1 << 29) | (0x09 << 16) | 1;
        User32.INSTANCE.PostMessage(s.gameHwnd, WM_SYSKEYDOWN, new WPARAM(VK_8), new LPARAM(lParamDown));
        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        User32.INSTANCE.PostMessage(s.gameHwnd, WM_SYSKEYUP, new WPARAM(VK_8), new LPARAM(lParamUp));
        System.out.println("📩 后台指令投递完毕！");
    }

    private static class TrackerState {
        private int windowBaseX = -1;
        private int windowBaseY = -1;
        private String fullWindowTitle = "";
        private HWND gameHwnd = null;
    }
}
