package com.bot.dhxy.ui.hotkey;

import com.bot.dhxy.runner.control.TaskControlService;
import com.bot.dhxy.window.control.WindowTaskControlService;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Windows 全局紧急停止热键。
 *
 * 真实鼠标被游戏任务占用时，用户不一定能方便地点到 JavaFX 控制台按钮。
 * 这里注册系统级 Alt + F12，用键盘直接请求停止单窗口任务和多窗口任务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalEmergencyStopHotkeyService {

    private static final int HOTKEY_ID = 0x4458;
    private static final int WM_HOTKEY = 0x0312;
    private static final int MOD_ALT = 0x0001;
    private static final int VK_F12 = 0x7B;

    private final TaskControlService taskControlService;
    private final WindowTaskControlService windowTaskControlService;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread hotkeyThread;

    @PostConstruct
    public void start() {
        if (!isWindows()) {
            log.info("当前系统不是 Windows，跳过全局紧急停止热键注册。");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }

        hotkeyThread = new Thread(this::runHotkeyLoop, "global-emergency-stop-hotkey");
        hotkeyThread.setDaemon(true);
        hotkeyThread.start();
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        try {
            User32.INSTANCE.UnregisterHotKey(null, HOTKEY_ID);
        } catch (Exception e) {
            log.debug("释放全局紧急停止热键时发生异常，忽略。", e);
        }
        if (hotkeyThread != null) {
            hotkeyThread.interrupt();
        }
    }

    private void runHotkeyLoop() {
        boolean registered = User32.INSTANCE.RegisterHotKey(null, HOTKEY_ID, MOD_ALT, VK_F12);
        if (!registered) {
            log.warn("全局紧急停止热键注册失败：Alt + F12。可能已被其他程序占用。");
            running.set(false);
            return;
        }

        log.info("✅ 全局紧急停止热键已注册：Alt + F12");
        WinUser.MSG msg = new WinUser.MSG();
        while (running.get()) {
            int result = User32.INSTANCE.GetMessage(msg, (WinDef.HWND) null, 0, 0);
            if (result <= 0) {
                break;
            }
            if (msg.message == WM_HOTKEY && msg.wParam != null && msg.wParam.intValue() == HOTKEY_ID) {
                triggerEmergencyStop();
            }
        }

        User32.INSTANCE.UnregisterHotKey(null, HOTKEY_ID);
        running.set(false);
        log.info("全局紧急停止热键监听已退出。");
    }

    private void triggerEmergencyStop() {
        log.warn("🚨 收到全局紧急停止热键 Alt + F12，正在请求停止所有任务。");
        try {
            taskControlService.stop();
        } catch (Exception e) {
            log.warn("请求停止单窗口任务时发生异常。", e);
        }
        try {
            windowTaskControlService.stopAll();
        } catch (Exception e) {
            log.warn("请求停止多窗口任务时发生异常。", e);
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }
}
