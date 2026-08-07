package com.bot.dhxy.input;

import java.util.concurrent.atomic.AtomicInteger;

/** Connectivity guard from the Windows hotkey dispatch to the shared UI lifecycle callback. */
public final class GlobalEmergencyStopHotkeyServiceConnectivityTest {

    public static void main(String[] args) {
        AtomicInteger pauseCalls = new AtomicInteger();
        AtomicInteger resumeCalls = new AtomicInteger();
        AtomicInteger stopCalls = new AtomicInteger();
        GlobalEmergencyStopHotkeyService runningService = new GlobalEmergencyStopHotkeyService(
                () -> {
                    pauseCalls.incrementAndGet();
                    return true;
                },
                resumeCalls::incrementAndGet,
                stopCalls::incrementAndGet);

        runningService.triggerPauseAll();
        require(pauseCalls.get() == 1, "F11 must attempt the immediate non-FX pause path first");
        require(resumeCalls.get() == 0, "a successful immediate pause must not dispatch resume");
        require(stopCalls.get() == 0, "F11 must not dispatch the F12 stop callback");

        runningService.triggerEmergencyStop();
        require(pauseCalls.get() == 1, "F12 must not dispatch the F11 pause callback");
        require(stopCalls.get() == 1, "F12 must retain the direct global stop callback");

        GlobalEmergencyStopHotkeyService pausedService = new GlobalEmergencyStopHotkeyService(
                () -> {
                    pauseCalls.incrementAndGet();
                    return false;
                },
                resumeCalls::incrementAndGet,
                stopCalls::incrementAndGet);
        pausedService.triggerPauseAll();
        require(pauseCalls.get() == 2, "F11 must inspect the immediate pause path before deciding to resume");
        require(resumeCalls.get() == 1, "F11 must dispatch resume when there are no running windows to pause");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
