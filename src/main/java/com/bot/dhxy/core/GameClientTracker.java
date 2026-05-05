package com.bot.dhxy.core;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.config.InputProvider;
import com.bot.dhxy.config.VisionProvider;
import com.bot.dhxy.tools.CoordinateHelper;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired; // 🌟 新增引入
import org.springframework.context.annotation.Lazy; // 🌟 新增引入
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

@Component
@RequiredArgsConstructor
public class GameClientTracker {

    private final VisionProvider eyes;
    private final BotProperties config;
    private final CoordinateHelper coordinateHelper;

    // ==========================================
    // 🌟 核心破局点：去掉 final，改用 @Lazy 延迟注入，打破死循环！
    // ==========================================
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

    /**
     * 🌍 刷新全局视野（每次任务循环/决策前，调用一次即可）
     */
    public boolean updateGlobalVision() {
        if (!checkBaseAddress()) return false;

        int x1 = windowBaseX;
        int y1 = windowBaseY;
        int x2 = x1 + WINDOW_WIDTH;
        int y2 = y1 + WINDOW_HEIGHT;

        return captureToFile("全局视野", LATEST_VISION_PATH, x1, y1, x2, y2);
    }

    /**
     * 🛰️ 定位并锁定游戏窗口
     */
    public boolean locateWindow() {
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

        RECT rect = new RECT();
        User32.INSTANCE.GetWindowRect(targetHwnd[0], rect);

        double scale = coordinateHelper.getScaleRatio();
        windowBaseX = (int) (rect.left / scale);
        windowBaseY = (int) (rect.top / scale);

        System.out.println("✅ [定位成功] 目标: " + fullWindowTitle + " | 窗口基址 X:" + windowBaseX + " Y:" + windowBaseY);
        return true;
    }

    // ==========================================
    // 📸 极简截图接口
    // ==========================================

    public boolean captureToFile(String elementName, String savePath, int x1, int y1, int x2, int y2) {
        if (!checkBaseAddress()) return false;
        System.out.println("📸 [" + elementName + "] 正在截取画面保存至: " + savePath);
        return eyes.captureRegionToFile(savePath, x1, y1, x2, y2);
    }

    // ========================================================================
    // 🛡️ 2. 装甲版照相机 (自带 ALT+4 物理清屏)
    // ========================================================================
    public boolean captureToFileWithShield(String elementName, String savePath, int x1, int y1, int x2, int y2) {
        if (!checkBaseAddress()) return false;
        System.out.println("🛡️ [装甲截图] 准备截取 " + elementName + ": 启动强制清屏 (ALT+4)...");

        // 1. 开盾
        inputProvider.pressAlt4();

        // 🌟 必须等400毫秒卸载模型，防止残影！
        sleepQuietly(400);

        try {
            // 2. 调用基础截图保存文件
            return eyes.captureRegionToFile(savePath, x1, y1, x2, y2);
        } finally {
            // 3. 关盾恢复
            inputProvider.pressAlt4();
            System.out.println("🔰 [装甲截图] " + elementName + " 截图完毕，画面已恢复。");
        }
    }

    public BufferedImage captureToMemory(String elementName, int x1, int y1, int x2, int y2) {
        if (!checkBaseAddress()) return null;
        return eyes.captureRegionByCoordinates(x1, y1, x2, y2);
    }

    private boolean checkBaseAddress() {
        if (windowBaseX == -1 || gameHwnd == null) {
            return locateWindow();
        }
        RECT rect = new RECT();
        User32.INSTANCE.GetWindowRect(gameHwnd, rect);

        double scale = coordinateHelper.getScaleRatio();
        windowBaseX = (int) (rect.left / scale);
        windowBaseY = (int) (rect.top / scale);

        return true;
    }

    public boolean bringWindowToFront() {
        if (gameHwnd == null) {
            if (!locateWindow()) return false;
        }
        System.out.println("🔄 正在将游戏窗口唤醒并置顶...");
        User32.INSTANCE.ShowWindow(gameHwnd, 9);
        User32.INSTANCE.SetForegroundWindow(gameHwnd);
        sleepQuietly(500);
        return true;
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}