package com.bot.dhxy.input;

import com.bot.dhxy.ui.MainWindowController;
import com.bot.dhxy.window.control.WindowTaskControlService;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Windows global task-control hotkeys.
 *
 * Ctrl + Shift + F11 toggles pause/resume for all registered window tasks.
 * Ctrl + Shift + F12 stops all registered window tasks.
 */
@Slf4j
@Service("inputGlobalEmergencyStopHotkeyService")
public class GlobalEmergencyStopHotkeyService {

    private static final int HOTKEY_ID_PAUSE_ALL = 0x0F11;
    private static final int HOTKEY_ID_EMERGENCY_STOP = 0x0F12;
    private static final int MOD_CONTROL = 0x0002;
    private static final int MOD_SHIFT = 0x0004;
    private static final int MOD_NOREPEAT = 0x4000;
    private static final int TASK_CONTROL_MODIFIERS = MOD_CONTROL | MOD_SHIFT | MOD_NOREPEAT;
    private static final int VK_F11 = 0x7A;
    private static final int VK_F12 = 0x7B;
    private static final String PAUSE_HOTKEY_TEXT = "Ctrl+Shift+F11";
    private static final String STOP_HOTKEY_TEXT = "Ctrl+Shift+F12";
    private static final long HOTKEY_DEBOUNCE_NANOS = java.time.Duration.ofMillis(500L).toNanos();

    private final BooleanSupplier pauseRunningAction;
    private final Runnable resumePausedAction;
    private final Runnable stopAllAction;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong lastPauseTriggerNanos = new AtomicLong(Long.MIN_VALUE);
    private final AtomicLong lastStopTriggerNanos = new AtomicLong(Long.MIN_VALUE);

    private Thread hotkeyThread;

    @Autowired
    public GlobalEmergencyStopHotkeyService(WindowTaskControlService windowTaskControlService,
                                             MainWindowController mainWindowController) {
        this(() -> pauseRunningWindows(windowTaskControlService),
                mainWindowController::handleGlobalPauseResumeHotkey,
                windowTaskControlService::stopAll);
    }

    GlobalEmergencyStopHotkeyService(BooleanSupplier pauseRunningAction,
                                     Runnable resumePausedAction,
                                     Runnable stopAllAction) {
        this.pauseRunningAction = Objects.requireNonNull(pauseRunningAction, "pauseRunningAction");
        this.resumePausedAction = Objects.requireNonNull(resumePausedAction, "resumePausedAction");
        this.stopAllAction = Objects.requireNonNull(stopAllAction, "stopAllAction");
    }

    public void start() {
        if (!isWindows()) {
            log.info("Current system is not Windows, skip global task hotkey registration.");
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
            User32.INSTANCE.UnregisterHotKey(null, HOTKEY_ID_PAUSE_ALL);
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
        boolean pauseRegistered = false;
        boolean stopRegistered = false;
        try {
            pauseRegistered = User32.INSTANCE.RegisterHotKey(null, HOTKEY_ID_PAUSE_ALL, TASK_CONTROL_MODIFIERS, VK_F11);
            if (!pauseRegistered) {
                log.warn("{} pause hotkey registration failed; it may be used by another program.", PAUSE_HOTKEY_TEXT);
            }
            stopRegistered = User32.INSTANCE.RegisterHotKey(null, HOTKEY_ID_EMERGENCY_STOP, TASK_CONTROL_MODIFIERS, VK_F12);
            if (!stopRegistered) {
                log.warn("{} emergency stop hotkey registration failed; it may be used by another program.", STOP_HOTKEY_TEXT);
            }
            if (!pauseRegistered && !stopRegistered) {
                return;
            }
            log.info("Registered global task hotkeys: pause={} stop={}", pauseRegistered, stopRegistered);

            WinUser.MSG msg = new WinUser.MSG();
            while (running.get()) {
                int result = User32.INSTANCE.GetMessage(msg, (WinDef.HWND) null, 0, 0);
                if (result <= 0) {
                    break;
                }
                if (msg.message == WinUser.WM_HOTKEY) {
                    int hotkeyId = msg.wParam.intValue();
                    if (hotkeyId == HOTKEY_ID_PAUSE_ALL) {
                        triggerPauseAll();
                    } else if (hotkeyId == HOTKEY_ID_EMERGENCY_STOP) {
                        triggerEmergencyStop();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Global task hotkey thread error", e);
        } finally {
            if (pauseRegistered) {
                User32.INSTANCE.UnregisterHotKey(null, HOTKEY_ID_PAUSE_ALL);
            }
            if (stopRegistered) {
                User32.INSTANCE.UnregisterHotKey(null, HOTKEY_ID_EMERGENCY_STOP);
            }
            running.set(false);
            started.set(false);
            log.info("Global task hotkeys released.");
        }
    }

    void triggerPauseAll() {
        if (!claimTrigger(lastPauseTriggerNanos)) {
            return;
        }
        log.warn("{} pause/resume hotkey triggered: toggling all window tasks...", PAUSE_HOTKEY_TEXT);
        try {
            if (!pauseRunningAction.getAsBoolean()) {
                resumePausedAction.run();
            }
        } catch (Exception e) {
            log.warn("{} pause/resume hotkey failed while toggling window tasks", PAUSE_HOTKEY_TEXT, e);
        }
        log.warn("{} pause/resume request sent.", PAUSE_HOTKEY_TEXT);
    }

    private static boolean pauseRunningWindows(WindowTaskControlService windowTaskControlService) {
        var runningWindowIds = windowTaskControlService.getSnapshots().stream()
                .filter(snapshot -> snapshot.isRunning())
                .map(snapshot -> snapshot.getWindowId())
                .toList();
        if (runningWindowIds.isEmpty()) {
            return false;
        }
        log.warn("{} dispatching immediate pause outside JavaFX: windows={}",
                PAUSE_HOTKEY_TEXT, runningWindowIds);
        var result = windowTaskControlService.pauseWindows(runningWindowIds);
        log.warn("{} immediate pause completed: successCount={} requestedCount={} message={}",
                PAUSE_HOTKEY_TEXT, result.getSuccessCount(), result.getRequestedCount(), result.getMessage());
        return true;
    }

    void triggerEmergencyStop() {
        if (!claimTrigger(lastStopTriggerNanos)) {
            return;
        }
        log.warn("{} emergency stop triggered: stopping all window tasks...", STOP_HOTKEY_TEXT);
        try {
            stopAllAction.run();
        } catch (Exception e) {
            log.warn("{} emergency stop failed while stopping window tasks", STOP_HOTKEY_TEXT, e);
        }
        log.warn("{} emergency stop request sent.", STOP_HOTKEY_TEXT);
    }

    private static boolean claimTrigger(AtomicLong lastTriggerNanos) {
        long now = System.nanoTime();
        while (true) {
            long previous = lastTriggerNanos.get();
            if (previous != Long.MIN_VALUE && now - previous < HOTKEY_DEBOUNCE_NANOS) {
                return false;
            }
            if (lastTriggerNanos.compareAndSet(previous, now)) {
                return true;
            }
        }
    }

    private boolean isWindows() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("windows");
    }
}
