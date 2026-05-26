package com.bot.dhxy.input;

import com.bot.dhxy.window.control.WindowTaskControlService;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Windows global emergency stop hotkey.
 *
 * Ctrl + Shift + F12 stops all registered window tasks.
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

    private final WindowTaskControlService windowTaskControlService;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Thread hotkeyThread;

    public GlobalEmergencyStopHotkeyService(WindowTaskControlService windowTaskControlService) {
        this.windowTaskControlService = windowTaskControlService;
    }

    public void start() {
        if (!isWindows()) {
            log.info("Current system is not Windows, skip {} emergency stop hotkey registration.", HOTKEY_TEXT);
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
            // JVM shutdown should not surface extra hotkey cleanup noise.
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
                log.warn("{} emergency stop hotkey registration failed; it may be used by another program.", HOTKEY_TEXT);
                return;
            }
            log.info("Registered global emergency stop hotkey: {}", HOTKEY_TEXT);

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
            log.error("{} emergency stop hotkey thread error", HOTKEY_TEXT, e);
        } finally {
            if (registered) {
                User32.INSTANCE.UnregisterHotKey(null, HOTKEY_ID_EMERGENCY_STOP);
            }
            running.set(false);
            started.set(false);
            log.info("{} emergency stop hotkey released.", HOTKEY_TEXT);
        }
    }

    private void triggerEmergencyStop() {
        log.warn("{} emergency stop triggered: stopping all window tasks...", HOTKEY_TEXT);
        try {
            windowTaskControlService.stopAll();
        } catch (Exception e) {
            log.warn("{} emergency stop failed while stopping window tasks", HOTKEY_TEXT, e);
        }
        log.warn("{} emergency stop request sent.", HOTKEY_TEXT);
    }

    private boolean isWindows() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("windows");
    }
}
