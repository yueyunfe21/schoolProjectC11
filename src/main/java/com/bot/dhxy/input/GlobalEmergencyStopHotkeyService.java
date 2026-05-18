package com.bot.dhxy.input;

import com.bot.dhxy.runner.control.TaskControlService;
import com.bot.dhxy.window.control.WindowTaskControlService;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Windows 全局紧急停止热键。
 *
 * Ctrl + Shift + F12 用于在游戏抢占鼠标时快速停止全部任务。
 */
@Slf4j
@Service("inputGlobalEmergencyStopHotkeyService")
public class GlobalEmergencyStopHotkeyService {

    private static final int HOTKEY_ID_EMERGENCY_STOP = 0x0F12;
    private static final int MOD_CONTROL = 0x0002;
    private static final int MOD_SHIFT = 0x0004;
    private static final int MOD_NOREPEAT = 0x4000;
    private static final int EMERGENCY_STOP_MODIFIERS = MOD_CONTROL | MOD_SHIFT | MOD_NOREPEAT;
    private static final int VK_F12 = 0x7B;
    private static final String HOTKEY_TEXT = "Ctrl+Shift+F12";

    private final TaskControlService taskControlService;
    private final WindowTaskControlService windowTaskControlService;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Thread hotkeyThread;

    public GlobalEmergencyStopHotkeyService(TaskControlService taskControlService,
                                            WindowTaskControlService windowTaskControlService) {
        this.taskControlService = taskControlService;
        this.windowTaskControlService = windowTaskControlService;
    }

    public void start() {
        if (!isWindows()) {
            log.info("当前系统不是 Windows，跳过 {} 全局紧急停止热键注册。", HOTKEY_TEXT);
            return;
        }
        if (!started.compareAndSet(false, true)) {
            return;
        }
        running.set(true);
        hotkeyThread = new Thread(this::runHotkeyLoop, "global-emergency-stop-hotkey");
        hotkeyThread.setDaemon(true);
        hotkeyThread.start();
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        started.set(false);
        try {
            User32.INSTANCE.UnregisterHotKey(null, HOTKEY_ID_EMERGENCY_STOP);
        } catch (Exception ignored) {
            // JVM 退出阶段不再抛出额外异常。
        }
        if (hotkeyThread != null) {
            hotkeyThread.interrupt();
        }
    }

    public boolean isStarted() {
        return started.get();
    }

    private void runHotkeyLoop() {
        boolean registered = false;
        try {
            registered = User32.INSTANCE.RegisterHotKey(null, HOTKEY_ID_EMERGENCY_STOP, EMERGENCY_STOP_MODIFIERS, VK_F12);
            if (!registered) {
                log.warn("{} 全局紧急停止热键注册失败，可能已被其他程序占用。", HOTKEY_TEXT);
                return;
            }
            log.info("✅ 已注册全局紧急停止热键：{}", HOTKEY_TEXT);

            WinUser.MSG msg = new WinUser.MSG();
            while (running.get()) {
                int result = User32.INSTANCE.GetMessage(msg, (WinDef.HWND) null, 0, 0);
                if (result <= 0) {
                    break;
                }
                if (msg.message == WinUser.WM_HOTKEY && msg.wParam.intValue() == HOTKEY_ID_EMERGENCY_STOP) {
                    triggerEmergencyStop();
                }
            }
        } catch (Exception e) {
            log.error("{} 全局紧急停止热键线程异常", HOTKEY_TEXT, e);
        } finally {
            if (registered) {
                User32.INSTANCE.UnregisterHotKey(null, HOTKEY_ID_EMERGENCY_STOP);
            }
            running.set(false);
            started.set(false);
            log.info("{} 全局紧急停止热键已释放。", HOTKEY_TEXT);
        }
    }

    private void triggerEmergencyStop() {
        log.warn("🛑 {} 紧急停止触发：正在请求停止单窗口任务和所有多窗口任务...", HOTKEY_TEXT);
        try {
            taskControlService.stop();
        } catch (Exception e) {
            log.warn("{} 紧急停止：停止单窗口任务时出现异常", HOTKEY_TEXT, e);
        }
        try {
            windowTaskControlService.stopAll();
        } catch (Exception e) {
            log.warn("{} 紧急停止：停止多窗口任务时出现异常", HOTKEY_TEXT, e);
        }
        log.warn("🛑 {} 紧急停止请求已发送。", HOTKEY_TEXT);
    }

    private boolean isWindows() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("windows");
    }
}
