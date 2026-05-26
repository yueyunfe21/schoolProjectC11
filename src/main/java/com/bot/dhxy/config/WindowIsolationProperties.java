package com.bot.dhxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 多窗口隔离功能开关。
 *
 * 默认全部关闭，优先保证旧的单窗口五环稳定。
 * 多窗口测试时必须逐项打开，不能一次把所有隔离能力全开。
 */
@Data
@Component
@ConfigurationProperties(prefix = "bot.window")
public class WindowIsolationProperties {

    /**
     * 多窗口隔离总开关。
     * false 时所有细分隔离能力都视为关闭。
     */
    private boolean isolationEnabled = false;

    /**
     * 输入前是否根据当前 WindowTaskRunner 绑定的 hwnd 激活窗口。
     */
    private boolean inputFocusEnabled = false;

    /**
     * GameClientTracker 是否按窗口任务线程隔离 gameHwnd / windowBaseX / windowBaseY。
     */
    private boolean trackerStateIsolationEnabled = false;

    /**
     * GameClientTracker 是否优先使用当前窗口绑定的 native hwnd，而不是按标题搜索第一个窗口。
     */
    private boolean boundWindowTrackerEnabled = false;

    /**
     * 截图文件是否按 windowId 分目录。当前默认必须关闭，否则旧识别代码会读不到图。
     */
    private boolean scopedTempPathEnabled = false;

    /**
     * 是否在绑定窗口上下文中优先使用 Win32 HWND 截图，避免 Robot 被遮挡窗口污染。
     */
    private boolean hwndCaptureEnabled = false;

    /**
     * HWND 截图失败时是否退回 Robot 可见屏幕截图。
     */
    private boolean hwndCaptureFallbackToRobotEnabled = true;

    /**
     * 是否允许已验证的键盘快捷键通过 HWND 后台消息发送。
     */
    private boolean hwndKeyboardEnabled = false;

    public boolean isInputFocusActive() {
        return isolationEnabled && inputFocusEnabled;
    }

    public boolean isTrackerStateIsolationActive() {
        return isolationEnabled && trackerStateIsolationEnabled;
    }

    public boolean isBoundWindowTrackerActive() {
        return isolationEnabled && boundWindowTrackerEnabled;
    }

    public boolean isScopedTempPathActive() {
        return isolationEnabled && scopedTempPathEnabled;
    }

    public boolean isHwndCaptureActive() {
        return isolationEnabled && boundWindowTrackerEnabled && hwndCaptureEnabled;
    }

    public boolean isHwndCaptureFallbackToRobotActive() {
        return !isHwndCaptureActive() || hwndCaptureFallbackToRobotEnabled;
    }

    public boolean isHwndKeyboardActive() {
        return isolationEnabled && boundWindowTrackerEnabled && hwndKeyboardEnabled;
    }
}
