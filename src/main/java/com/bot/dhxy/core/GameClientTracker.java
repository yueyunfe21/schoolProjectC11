package com.bot.dhxy.core;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.config.VisionProvider;
import com.bot.dhxy.tools.CoordinateHelper; // 🌟 引入刚写好的助手
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

@Component
@RequiredArgsConstructor
public class GameClientTracker {

    private final VisionProvider eyes;
    private final BotProperties config;
    private final CoordinateHelper coordinateHelper; // 🌟 1. 注入全自动雷达

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


    // ... 在类里加上这个公共方法
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

        // ==========================================
        // 🌟 2. 核心修改：使用 CoordinateHelper 的全自动比例！
        // ==========================================
        double scale = coordinateHelper.getScaleRatio();
        windowBaseX = (int) (rect.left / scale);
        windowBaseY = (int) (rect.top / scale);

        System.out.println("✅ [定位成功] 目标: " + fullWindowTitle + " | 窗口基址 X:" + windowBaseX + " Y:" + windowBaseY);
        return true;
    }


    // ==========================================
    // 📸 极简截图接口 (坐标统一由 CoordinateHelper 提供)
    // ==========================================

    /**
     * 💾 截取指定区域并【保存到硬盘】
     */
    public boolean captureToFile(String elementName, String savePath, int x1, int y1, int x2, int y2) {
        if (!checkBaseAddress()) return false;
        System.out.println("📸 [" + elementName + "] 正在截取画面保存至: " + savePath);

        return eyes.captureRegionToFile(savePath, x1, y1, x2, y2);
    }

    /**
     * 🧠 截取指定区域到【内存】
     */
    public BufferedImage captureToMemory(String elementName, int x1, int y1, int x2, int y2) {
        if (!checkBaseAddress()) return null;

        return eyes.captureRegionByCoordinates(x1, y1, x2, y2);
    }

    // ==========================================

    private boolean checkBaseAddress() {
        // 1. 如果还没定位过，走全量搜索
        if (windowBaseX == -1 || gameHwnd == null) {
            return locateWindow();
        }

        // 2. 🌟 实时刷新：如果已经有了句柄，每次都快速获取最新物理位置！
        // 这一步极快，完全不会影响性能
        RECT rect = new RECT();
        User32.INSTANCE.GetWindowRect(gameHwnd, rect);

        double scale = coordinateHelper.getScaleRatio();
        windowBaseX = (int) (rect.left / scale);
        windowBaseY = (int) (rect.top / scale);

        return true;
    }

    /**
     * 👊 强制将游戏窗口拉到屏幕最前方
     */
    public boolean bringWindowToFront() {
        if (gameHwnd == null) {
            if (!locateWindow()) return false;
        }
        System.out.println("🔄 正在将游戏窗口唤醒并置顶...");
        User32.INSTANCE.ShowWindow(gameHwnd, 9);
        User32.INSTANCE.SetForegroundWindow(gameHwnd);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }
}
